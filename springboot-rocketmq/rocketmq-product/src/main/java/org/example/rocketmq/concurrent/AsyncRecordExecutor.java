package org.example.rocketmq.concurrent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步落库执行器（JUC 线程池）——消息记录「不影响主链路性能」的关键设计。
 *
 * <p><b>用途</b>：把<b>非关键</b>的数据库写操作（如发送成功后的状态回写、消费成功后的状态回写）
 * 从 RocketMQ 的发送/消费线程中剥离，交给独立线程池异步执行，使主链路尽快返回，提升吞吐。</p>
 *
 * <p><b>线程池选型（生产可交付）</b>：</p>
 * <ul>
 *     <li><b>有界队列</b> {@link ArrayBlockingQueue}：防止任务无限堆积导致 OOM；</li>
 *     <li><b>拒绝策略 CallerRunsPolicy</b>：队列满时由<b>调用线程</b>自己执行，形成天然背压（back-pressure），
 *         既不丢任务、又能自动降速，避免压垮数据库；</li>
 *     <li><b>命名线程工厂</b>：便于在线程 dump / 监控中定位；</li>
 *     <li><b>优雅停机</b>：{@link #shutdown()} 在容器销毁时等待在途任务完成，避免状态丢失。</li>
 * </ul>
 *
 * <p><b>哪些操作走异步、哪些必须同步</b>（见 {@code MessageRecordService} / {@code MessageReliabilityService}）：</p>
 * <ul>
 *     <li><b>同步</b>：发送前落库 PENDING、消费前落库（幂等）、失败落重试表 —— 关乎可靠性，绝不能丢；</li>
 *     <li><b>异步</b>：发送/消费<b>成功</b>后的状态回写 —— 即便偶发丢失，记录停留在 PENDING/CONSUMING，
 *         可由补偿任务修正，不影响业务正确性。</li>
 * </ul>
 *
 * @author demo
 */
@Slf4j
@Component
public class AsyncRecordExecutor {

    /** 核心线程数 */
    @Value("${reliability.async.core-pool-size:4}")
    private int corePoolSize;

    /** 最大线程数 */
    @Value("${reliability.async.max-pool-size:8}")
    private int maxPoolSize;

    /** 有界队列容量 */
    @Value("${reliability.async.queue-capacity:2000}")
    private int queueCapacity;

    /** 非核心线程空闲存活时间（秒） */
    @Value("${reliability.async.keep-alive-seconds:60}")
    private long keepAliveSeconds;

    /** 底层线程池：有界队列 + CallerRunsPolicy 背压 */
    private ThreadPoolExecutor executor;

    /** 已提交任务数（监控用） */
    private final AtomicLong submitted = new AtomicLong();

    /** 已完成任务数（监控用） */
    private final AtomicLong completed = new AtomicLong();

    /** 任务异常数（监控用） */
    private final AtomicLong failed = new AtomicLong();

    /**
     * 懒初始化线程池（首次使用时创建），避免 @Value 注入时机问题。
     */
    private ThreadPoolExecutor executor() {
        ThreadPoolExecutor e = this.executor;
        if (e == null) {
            synchronized (this) {
                e = this.executor;
                if (e == null) {
                    e = new ThreadPoolExecutor(
                            corePoolSize, maxPoolSize,
                            keepAliveSeconds, TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(queueCapacity),
                            new NamedThreadFactory("mq-async-record-"),
                            // 队列满时由调用线程执行，形成背压、不丢任务
                            new ThreadPoolExecutor.CallerRunsPolicy());
                    this.executor = e;
                    log.info("[异步落库] 线程池初始化: core={}, max={}, queue={}",
                            corePoolSize, maxPoolSize, queueCapacity);
                }
            }
        }
        return e;
    }

    /**
     * 提交一个异步任务（无返回值）。任务内部异常会被捕获并计数，绝不向上抛出影响主链路。
     *
     * @param task 待执行任务（通常是一次状态回写）
     */
    public void submit(Runnable task) {
        submitted.incrementAndGet();
        try {
            executor().execute(() -> {
                try {
                    task.run();
                    completed.incrementAndGet();
                } catch (Throwable t) {
                    failed.incrementAndGet();
                    // 异步任务异常不能影响主链路，仅记录日志
                    log.error("[异步落库] 任务执行异常: {}", t.getMessage(), t);
                }
            });
        } catch (Throwable t) {
            // 理论上 CallerRunsPolicy 不会拒绝，这里兜底：降级为同步执行，保证不丢
            failed.incrementAndGet();
            log.error("[异步落库] 提交失败，降级同步执行: {}", t.getMessage(), t);
            task.run();
        }
    }

    /**
     * 提交一个有返回值的异步任务（基于 {@link CompletableFuture}）。
     *
     * @param task 待执行任务
     * @param <T>  返回类型
     * @return CompletableFuture，调用方可选择性 join
     */
    public <T> CompletableFuture<T> supply(java.util.function.Supplier<T> task) {
        submitted.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.get();
                completed.incrementAndGet();
                return result;
            } catch (Throwable t) {
                failed.incrementAndGet();
                log.error("[异步落库] supply 任务异常: {}", t.getMessage(), t);
                throw t;
            }
        }, executor());
    }

    /** 监控快照：提交数 / 完成数 / 失败数 / 队列积压 / 活跃线程 */
    public String stats() {
        ThreadPoolExecutor e = this.executor;
        return String.format("submitted=%d, completed=%d, failed=%d, queueSize=%d, activeCount=%d",
                submitted.get(), completed.get(), failed.get(),
                e == null ? 0 : e.getQueue().size(),
                e == null ? 0 : e.getActiveCount());
    }

    /**
     * 容器销毁时优雅停机：先停止接收新任务，再等待在途任务完成，避免状态回写丢失。
     */
    @PreDestroy
    public void shutdown() {
        ThreadPoolExecutor e = this.executor;
        if (e == null) {
            return;
        }
        e.shutdown();
        try {
            // 最多等待 30 秒让在途任务完成
            if (!e.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[异步落库] 线程池未在 30s 内结束，强制关闭，可能丢失 {} 个在途任务",
                        e.shutdownNow().size());
            }
        } catch (InterruptedException ie) {
            e.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[异步落库] 线程池已关闭. {}", stats());
    }

    /**
     * 命名线程工厂：生成守护线程并统一命名，便于排查。
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicLong seq = new AtomicLong(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + seq.getAndIncrement());
            // 守护线程：不阻止 JVM 退出（停机时由 @PreDestroy 优雅关闭兜底）
            t.setDaemon(true);
            return t;
        }
    }
}
