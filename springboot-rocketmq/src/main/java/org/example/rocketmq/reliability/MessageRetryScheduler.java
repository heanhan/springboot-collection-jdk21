package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.entity.MqMessageRetry;
import org.example.rocketmq.enums.RetryStatus;
import org.example.rocketmq.repository.MqMessageRetryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一消息重试调度器（扫描 mq_message_retry 表）。
 *
 * <p><b>用途</b>：定时扫描统一重试表中「状态为 PENDING 且已到重试时间」的记录，
 * 交给 {@link MessageRetryService#executeRetry} 按类型重放：</p>
 * <ul>
 *     <li>{@code PRODUCE}：取原始报文重新发送到 broker（生产重试）；</li>
 *     <li>{@code CONSUME}：取原始报文按 topic 反查处理器重新执行业务（消费重试）。</li>
 * </ul>
 *
 * <p><b>为什么独立成表调度</b>：重试调度只扫描 mq_message_retry 这张「小表」，
 * 索引 {@code (retry_type, status, next_retry_time)} 命中快，不必在大业务表上过滤，效率更高。</p>
 *
 * <p><b>多实例并发安全</b>：捞取后先用「乐观抢占」
 * {@code compareAndSetStatus(PENDING→RETRYING)}，只有抢占成功（affected=1）的实例才执行重试，
 * 避免集群部署时多实例重复重试同一条消息。</p>
 *
 * <p><b>触发方式</b>：① {@code @Scheduled} 周期自动扫描；② 调用 {@link #retryNow()} 手动触发（便于演示）。</p>
 *
 * @author demo
 */
@Slf4j
@Component
public class MessageRetryScheduler {

    @Resource
    private MqMessageRetryRepository retryRepository;

    @Resource
    private MessageRetryService messageRetryService;

    /** 每次扫描最多处理条数，避免单次任务过重 */
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
            log.info("[消息重试-定时扫描] 本轮处理待重试消息 {} 条", handled);
        }
    }

    /**
     * 立即执行一轮重试扫描（也可由 REST 接口手动触发，方便演示）。
     *
     * @return 本轮实际抢占并执行重试的记录条数
     */
    public int retryNow() {
        // 捞出到期的 PENDING 记录（按 next_retry_time 升序，限制单轮条数）
        List<MqMessageRetry> retryable = retryRepository.findRetryable(
                RetryStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, retryBatchSize));
        if (retryable.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (MqMessageRetry retry : retryable) {
            // 乐观抢占：PENDING → RETRYING，多实例下只有一个能成功
            int affected = retryRepository.compareAndSetStatus(
                    retry.getId(), RetryStatus.PENDING, RetryStatus.RETRYING, LocalDateTime.now());
            if (affected == 0) {
                // 已被其他实例抢占或状态已变，跳过
                continue;
            }
            try {
                // 抢占成功后内存对象状态需同步为 RETRYING，再执行重试
                retry.setStatus(RetryStatus.RETRYING);
                messageRetryService.executeRetry(retry);
                count++;
            } catch (Exception e) {
                // executeRetry 内部已处理成功/失败状态流转，这里兜底防御未知异常
                log.error("[消息重试-执行异常] id={}, type={}, topic={}, bizKey={}, err={}",
                        retry.getId(), retry.getRetryType(), retry.getTopic(), retry.getBizKey(), e.getMessage(), e);
            }
        }
        return count;
    }
}
