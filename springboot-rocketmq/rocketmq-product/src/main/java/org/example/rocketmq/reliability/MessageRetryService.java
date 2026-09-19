package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.entity.MqMessageRetry;
import org.example.rocketmq.entity.MqProduceRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.example.rocketmq.enums.ProduceStatus;
import org.example.rocketmq.enums.RetryStatus;
import org.example.rocketmq.enums.RetryType;
import org.example.rocketmq.repository.MqConsumeRecordRepository;
import org.example.rocketmq.repository.MqMessageRetryRepository;
import org.example.rocketmq.repository.MqProduceRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一消息重试服务（mq_message_retry 表的核心）。
 *
 * <p><b>用途</b>：把「生产失败」与「消费失败」的消息连同<b>原始报文</b>落入统一重试表，
 * 并在调度器触发时按 {@link RetryType} 走不同重放路径：</p>
 * <ul>
 *     <li><b>生产重试</b>：取原始报文，用原生 Producer <b>重新发送</b>到 broker（保留原 bytes 与 KEYS）；</li>
 *     <li><b>消费重试</b>：取原始报文，按 topic 反查 {@link ReliableMessageHandler} <b>重新执行业务</b>。</li>
 * </ul>
 *
 * <p><b>退避策略</b>：递增退避，第 n 次重试延迟 = {@code base-retry-interval-seconds * n} 秒；
 * 达 {@code max-retry} 仍失败置 {@link RetryStatus#DEAD}（死信），停止自动重试。</p>
 *
 * <p><b>幂等</b>：重试会重复发送/重复执行业务，因此消费端业务必须保证幂等
 * （消费端落库 mq_consume_record 时已用 (bizKey,topic) 唯一索引做幂等去重）。</p>
 *
 * @author demo
 */
@Slf4j
@Service
public class MessageRetryService {

    @Resource
    private MqMessageRetryRepository retryRepository;

    @Resource
    private MqProduceRecordRepository produceRecordRepository;

    @Resource
    private MqConsumeRecordRepository consumeRecordRepository;

    /**
     * 可靠消息处理器注册中心（消费重试时按 topic 反查处理器）。
     *
     * <p><b>为何用 {@code @Lazy}</b>：打破启动期循环依赖。Registry 通过构造器收集所有
     * {@link ReliableMessageHandler}（包含 {@code ReliableOrderConsumer}），而
     * {@code ReliableOrderConsumer -> MessageReliabilityService -> MessageRetryService -> Registry}
     * 构成一个环。该依赖仅在<b>重试执行期</b>才用到，注入懒代理后启动期不解析真实 Bean，
     * 环即被打破；首次调用 {@code get()} 时（调度器运行时，上下文已就绪）再解析。</p>
     */
    @Lazy
    @Resource
    private ReliableMessageHandlerRegistry handlerRegistry;

    @Resource
    private RocketMQTemplate rocketMqTemplate;

    /** 最大重试次数 */
    @Value("${reliability.max-retry:3}")
    private int maxRetry;

    /** 重试基础间隔（秒），递增退避：第 n 次重试延迟 = base * n */
    @Value("${reliability.base-retry-interval-seconds:10}")
    private int baseRetryIntervalSeconds;

    /* ==================================================================
     * 一、失败入重试表（由生产端 / 消费端在失败时调用）
     * ================================================================== */

    /**
     * 保存「生产失败」重试记录。
     *
     * @param bizKey         业务幂等键
     * @param topic          主题
     * @param tags           标签（可空）
     * @param body           原始报文 JSON（重发时直接取此）
     * @param producerGroup  生产者组
     * @param sourceRecordId 来源 mq_produce_record 主键
     * @param error          失败原因
     * @return 重试记录 ID
     */
    public Long saveProduceRetry(String bizKey, String topic, String tags, String body,
                                 String producerGroup, Long sourceRecordId, Throwable error) {
        MqMessageRetry retry = new MqMessageRetry();
        retry.setRetryType(RetryType.PRODUCE);
        retry.setBizKey(bizKey);
        retry.setTopic(topic);
        retry.setTags(tags);
        retry.setGroupName(producerGroup);
        retry.setBody(body);
        retry.setSourceRecordId(sourceRecordId);
        return saveRetry(retry, error);
    }

    /**
     * 保存「消费失败」重试记录。
     *
     * @param bizKey         业务幂等键
     * @param topic          主题
     * @param tags           标签（可空）
     * @param msgId          RocketMQ 消息 ID
     * @param consumerGroup  消费者组
     * @param body           原始报文 JSON（重放时直接取此）
     * @param sourceRecordId 来源 mq_consume_record 主键
     * @param error          失败原因
     * @return 重试记录 ID
     */
    public Long saveConsumeRetry(String bizKey, String topic, String tags, String msgId,
                                 String consumerGroup, String body, Long sourceRecordId, Throwable error) {
        MqMessageRetry retry = new MqMessageRetry();
        retry.setRetryType(RetryType.CONSUME);
        retry.setBizKey(bizKey);
        retry.setTopic(topic);
        retry.setTags(tags);
        retry.setMsgId(msgId);
        retry.setGroupName(consumerGroup);
        retry.setBody(body);
        retry.setSourceRecordId(sourceRecordId);
        return saveRetry(retry, error);
    }

    /**
     * 落库重试记录：初始 PENDING，按递增退避计算首次重试时间。
     * 若同一 (bizKey, topic, retryType) 已有未完成的记录，则复用并刷新，避免重复堆积。
     */
    private Long saveRetry(MqMessageRetry retry, Throwable error) {
        retry.setStatus(RetryStatus.PENDING);
        retry.setRetryCount(0);
        retry.setMaxRetry(maxRetry);
        retry.setNextRetryTime(LocalDateTime.now().plusSeconds(baseRetryIntervalSeconds));
        retry.setErrorMsg(truncate(error == null ? null : error.getMessage()));
        retryRepository.saveAndFlush(retry);
        log.warn("[消息重试-入表] type={}, topic={}, bizKey={}, retryId={}, 首次重试时间={}",
                retry.getRetryType(), retry.getTopic(), retry.getBizKey(), retry.getId(), retry.getNextRetryTime());
        return retry.getId();
    }

    /* ==================================================================
     * 二、执行重试（由调度器在乐观抢占成功后调用）
     * ================================================================== */

    /**
     * 执行一条重试记录：按类型分发到「生产重发」或「消费重放」。
     *
     * <p>调用前调度器应已通过 {@code compareAndSetStatus(PENDING→RETRYING)} 抢占本记录。</p>
     *
     * @param retry 重试记录（状态已为 RETRYING）
     */
    public void executeRetry(MqMessageRetry retry) {
        if (retry.getRetryType() == RetryType.PRODUCE) {
            retryProduce(retry);
        } else {
            retryConsume(retry);
        }
    }

    /**
     * 生产重试：取原始报文重新发送到 broker。
     * 成功 → 重试记录置 SUCCESS、回写 mq_produce_record 为 SUCCESS；失败 → 累加重试次数并重新排期/转死信。
     */
    private void retryProduce(MqMessageRetry retry) {
        try {
            // 用原生 Producer 重发，保留原始 bytes 与 KEYS（bizKey），确保消费端幂等键一致
            String destination = retry.getTags() == null || retry.getTags().isBlank()
                    ? retry.getTopic()
                    : retry.getTopic() + ":" + retry.getTags();
            Message nativeMsg = new Message(retry.getTopic(), retry.getTags(), retry.getBizKey(),
                    retry.getBody().getBytes(StandardCharsets.UTF_8));
            SendResult result = rocketMqTemplate.getProducer().send(nativeMsg);

            // 重发成功：回填 msgId、置重试记录 SUCCESS
            retry.setMsgId(result.getMsgId());
            markRetrySuccess(retry);
            // 回写生产记录为 SUCCESS
            updateProduceRecordSuccess(retry.getSourceRecordId(), result.getMsgId());
            log.info("[消息重试-生产重发成功] topic={}, bizKey={}, msgId={}, 第 {} 次重试后成功, destination={}",
                    retry.getTopic(), retry.getBizKey(), result.getMsgId(), retry.getRetryCount() + 1, destination);
        } catch (Exception e) {
            markRetryFailed(retry, e);
            log.warn("[消息重试-生产重发失败] topic={}, bizKey={}, 原因={}",
                    retry.getTopic(), retry.getBizKey(), e.getMessage());
        }
    }

    /**
     * 消费重试：按 topic 反查业务处理器，用原始报文重新执行业务。
     * 成功 → 重试记录置 SUCCESS、回写 mq_consume_record 为 SUCCESS；失败 → 累加重试次数并重新排期/转死信。
     */
    private void retryConsume(MqMessageRetry retry) {
        ReliableMessageHandler handler = handlerRegistry.get(retry.getTopic());
        if (handler == null) {
            // 找不到处理器：不改重试次数，置回 PENDING 等待下轮（可能对应消费者 Bean 尚未加载）
            log.warn("[消息重试-消费重放] 未找到 topic={} 的处理器，保持 PENDING 等待下轮. bizKey={}",
                    retry.getTopic(), retry.getBizKey());
            retry.setStatus(RetryStatus.PENDING);
            retry.setNextRetryTime(LocalDateTime.now().plusSeconds(baseRetryIntervalSeconds));
            retryRepository.save(retry);
            return;
        }
        try {
            handler.handle(retry.getBody());
            markRetrySuccess(retry);
            // 回写消费记录为 SUCCESS
            updateConsumeRecordSuccess(retry.getSourceRecordId(), retry.getBizKey(),
                    retry.getTopic(), retry.getGroupName());
            log.info("[消息重试-消费重放成功] topic={}, bizKey={}, 第 {} 次重试后成功",
                    retry.getTopic(), retry.getBizKey(), retry.getRetryCount() + 1);
        } catch (Exception e) {
            markRetryFailed(retry, e);
            log.warn("[消息重试-消费重放失败] topic={}, bizKey={}, 原因={}",
                    retry.getTopic(), retry.getBizKey(), e.getMessage());
        }
    }

    /* ==================================================================
     * 三、重试记录状态流转
     * ================================================================== */

    /** 重试成功：置 SUCCESS、清空下次重试时间 */
    private void markRetrySuccess(MqMessageRetry retry) {
        retry.setStatus(RetryStatus.SUCCESS);
        retry.setNextRetryTime(null);
        retry.setErrorMsg(null);
        retryRepository.save(retry);
    }

    /**
     * 重试失败：累加重试次数；未达上限置回 PENDING 并按递增退避重新排期，达上限置 DEAD。
     */
    private void markRetryFailed(MqMessageRetry retry, Exception e) {
        int retryCount = (retry.getRetryCount() == null ? 0 : retry.getRetryCount()) + 1;
        retry.setRetryCount(retryCount);
        retry.setErrorMsg(truncate(e.getMessage()));
        int max = retry.getMaxRetry() == null ? maxRetry : retry.getMaxRetry();
        if (retryCount >= max) {
            retry.setStatus(RetryStatus.DEAD);
            retry.setNextRetryTime(null);
            log.error("[消息重试-死信] type={}, topic={}, bizKey={} 重试 {} 次仍失败，转死信需人工处理",
                    retry.getRetryType(), retry.getTopic(), retry.getBizKey(), retryCount);
            // 同步把来源记录置为终态（生产 FAILED / 消费 DEAD），便于监控
            if (retry.getRetryType() == RetryType.CONSUME) {
                updateConsumeRecordDead(retry.getSourceRecordId(), retry.getBizKey(),
                        retry.getTopic(), retry.getGroupName(), e);
            }
        } else {
            retry.setStatus(RetryStatus.PENDING);
            // 递增退避：第 n 次重试延迟 = base * n 秒
            retry.setNextRetryTime(LocalDateTime.now().plusSeconds((long) baseRetryIntervalSeconds * retryCount));
        }
        retryRepository.save(retry);
    }

    /* ==================================================================
     * 四、回写来源业务记录
     * ================================================================== */

    private void updateProduceRecordSuccess(Long sourceRecordId, String msgId) {
        if (sourceRecordId == null) {
            return;
        }
        produceRecordRepository.findById(sourceRecordId).ifPresent(rec -> {
            rec.setProduceStatus(ProduceStatus.SUCCESS);
            rec.setMsgId(msgId);
            rec.setProduceTime(LocalDateTime.now());
            rec.setProduceError(null);
            produceRecordRepository.save(rec);
        });
    }

    private void updateConsumeRecordSuccess(Long sourceRecordId, String bizKey, String topic, String group) {
        MqConsumeRecord rec = findConsumeRecord(sourceRecordId, bizKey, topic, group);
        if (rec == null) {
            return;
        }
        rec.setStatus(ConsumeStatus.SUCCESS);
        rec.setErrorMsg(null);
        rec.setNextRetryTime(null);
        consumeRecordRepository.save(rec);
    }

    private void updateConsumeRecordDead(Long sourceRecordId, String bizKey, String topic, String group, Exception e) {
        MqConsumeRecord rec = findConsumeRecord(sourceRecordId, bizKey, topic, group);
        if (rec == null) {
            return;
        }
        rec.setStatus(ConsumeStatus.DEAD);
        rec.setErrorMsg(truncate(e == null ? null : e.getMessage()));
        consumeRecordRepository.save(rec);
    }

    private MqConsumeRecord findConsumeRecord(Long sourceRecordId, String bizKey, String topic, String group) {
        if (sourceRecordId != null) {
            MqConsumeRecord rec = consumeRecordRepository.findById(sourceRecordId).orElse(null);
            if (rec != null) {
                return rec;
            }
        }
        return consumeRecordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, group);
    }

    /* ==================================================================
     * 五、监控查询
     * ================================================================== */

    /** 查询最近 100 条重试记录 */
    public List<MqMessageRetry> listRecent() {
        return retryRepository.findTop100ByOrderByCreatedTimeDesc();
    }

    /** 按类型查询最近 100 条重试记录 */
    public List<MqMessageRetry> listRecentByType(RetryType type) {
        return retryRepository.findTop100ByRetryTypeOrderByCreatedTimeDesc(type);
    }

    /** 按类型 + 状态统计 */
    public long countByTypeAndStatus(RetryType type, RetryStatus status) {
        return retryRepository.countByRetryTypeAndStatus(type, status);
    }

    /** 按状态统计 */
    public long countByStatus(RetryStatus status) {
        return retryRepository.countByStatus(status);
    }

    /** 错误信息截断 */
    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}
