package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.example.rocketmq.repository.MqConsumeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失败消息数据库重试调度器。
 *
 * <p><b>用途</b>：定时扫描本地消息表中「状态为 FAILED 且已到重试时间」的记录，
 * 通过 {@link ReliableMessageHandlerRegistry} 找到对应 topic 的业务处理器并重放业务逻辑。
 * 重试成功则置 SUCCESS，失败则累加重试次数并重新排期，达上限转 DEAD。</p>
 *
 * <p><b>为什么用数据库重试而非 broker 重试</b>：策略可控（次数/间隔/退避自定义）、
 * 过程可查（每条失败都有记录）、可人工干预（死信可捞出重发），且不受 broker 重试次数上限约束。</p>
 *
 * <p><b>触发方式</b>：① {@code @Scheduled} 周期自动扫描；② 调用 {@link #retryNow()} 手动触发（便于演示）。</p>
 *
 * <p><b>多实例注意</b>：集群部署时，多个实例的调度器会并发扫描，需用分布式锁或
 * 「乐观更新抢占」（先 update status=RETRYING where status=FAILED）避免重复重试。本 Demo 为单实例，未加锁。</p>
 *
 * @author demo
 */
@Slf4j
@Component
public class FailedMessageRetryScheduler {

    @Resource
    private MqConsumeRecordRepository recordRepository;

    @Resource
    private ReliableMessageHandlerRegistry handlerRegistry;

    @Resource
    private MessageReliabilityService reliabilityService;

    /** 每次扫描最多处理条数 */
    @Value("${reliability.retry-batch-size:50}")
    private int retryBatchSize;

    /**
     * 周期任务：按 application.yml 中 {@code reliability.retry-scan-interval-millis} 的间隔执行。
     * fixedDelay 表示「上一次执行结束后」再等待该间隔，避免任务重叠。
     */
    @Scheduled(fixedDelayString = "${reliability.retry-scan-interval-millis:10000}")
    public void scheduledRetry() {
        int handled = retryNow();
        if (handled > 0) {
            log.info("[可靠消息-定时重试] 本轮处理失败消息 {} 条", handled);
        }
    }

    /**
     * 立即执行一轮重试扫描（也可由 REST 接口手动触发，方便演示）。
     *
     * @return 本轮处理的记录条数
     */
    public int retryNow() {
        // 捞出到期的失败记录（按 next_retry_time 升序，限制单轮条数）
        List<MqConsumeRecord> retryable = recordRepository.findRetryable(
                ConsumeStatus.FAILED, LocalDateTime.now(), PageRequest.of(0, retryBatchSize));
        if (retryable.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MqConsumeRecord record : retryable) {
            ReliableMessageHandler handler = handlerRegistry.get(record.getTopic());
            if (handler == null) {
                // 找不到处理器：可能该消费者的 Bean 未加载，记录告警但不改状态，等待下轮
                log.warn("[可靠消息-定时重试] 未找到 topic={} 的处理器，跳过 bizKey={}",
                        record.getTopic(), record.getBizKey());
                continue;
            }
            reliabilityService.retryOne(record, handler);
            count++;
        }
        return count;
    }
}
