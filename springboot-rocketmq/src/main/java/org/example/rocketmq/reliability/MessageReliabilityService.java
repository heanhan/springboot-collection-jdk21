package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.concurrent.AsyncRecordExecutor;
import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.example.rocketmq.repository.MqConsumeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消费端消息可靠性服务（mq_consume_record 表的核心）。
 *
 * <p><b>职责边界</b>：本服务<b>只</b>负责消费端——「接收消息落库、消费成功/失败回写状态、
 * 失败入统一重试表」。生产端见 {@link MessageRecordService}（mq_produce_record），
 * 失败重试的执行见 {@link MessageRetryService}（mq_message_retry）。</p>
 *
 * <p><b>对应需求</b>：消费时接收消息即落库(CONSUMING)；消费成功/失败更新消费状态；
 * 消费失败连同<b>原始报文</b>录入统一重试表(type=CONSUME)，供后续「取原始数据重新消费」。</p>
 *
 * <p><b>同步/异步策略（不影响消费吞吐）</b>：</p>
 * <ul>
 *     <li><b>接收落库 CONSUMING —— 同步</b>：幂等去重必须在业务执行前完成，绝不能异步；</li>
 *     <li><b>消费成功回写 SUCCESS —— 异步</b>：交给 {@link AsyncRecordExecutor} 的 JUC 线程池，
 *         让消费线程尽快 ack 返回，提升吞吐；偶发丢失可由补偿修正；</li>
 *     <li><b>消费失败置 FAILED + 入重试表 —— 同步</b>：关乎可靠性，必须同步落库，避免丢失失败上下文。</li>
 * </ul>
 *
 * <p><b>与 broker 重试的区别</b>：{@link #consume} 失败时<b>不</b>把异常抛给 broker（即向 broker ack），
 * 而是记录到数据库重试表，由 {@code MessageRetryScheduler} 定时重放。这样重试策略完全可控、可观测、可持久，
 * 且避免 broker 重试与数据库重试「双重触发」。（broker 原生重试演示见 {@code RetryConsumer}。）</p>
 *
 * <p><b>幂等设计</b>：{@code (bizKey, topic, consumerGroup)} 唯一索引。首次消费插入成功才执行业务；
 * 插入冲突说明本组已处理过，进一步查状态：已 SUCCESS 则幂等跳过，否则复用原记录继续处理。</p>
 *
 * @author demo
 */
@Slf4j
@Service
public class MessageReliabilityService {

    /** 消费处理结果 */
    public enum ConsumeResult {
        /** 业务处理成功 */
        SUCCESS,
        /** 业务失败，已记录并写入统一重试表等待数据库重试 */
        FAILED_SCHEDULED_RETRY,
        /** 重复消息，已幂等跳过 */
        DUPLICATE_SKIPPED
    }

    @Resource
    private MqConsumeRecordRepository recordRepository;

    /** 统一重试服务：消费失败时把原始报文写入 mq_message_retry(type=CONSUME) */
    @Resource
    private MessageRetryService messageRetryService;

    /** 异步落库执行器：把「消费成功」的状态回写从消费线程剥离 */
    @Resource
    private AsyncRecordExecutor asyncRecordExecutor;

    /** 最大重试次数，来自 application.yml: reliability.max-retry */
    @Value("${reliability.max-retry:3}")
    private int maxRetry;

    /**
     * 可靠消费主流程：幂等落库(CONSUMING) → 执行业务 → 成功异步回写 / 失败同步入重试表。
     *
     * <p><b>注意</b>：本方法<b>不</b>加 {@code @Transactional}，且<b>吞掉</b>业务异常（返回
     * {@link ConsumeResult#FAILED_SCHEDULED_RETRY}），以便失败记录能独立写入数据库、且 broker 只投递一次。</p>
     *
     * @param topic         消息主题
     * @param consumerGroup 消费组
     * @param msgId         RocketMQ 消息 ID
     * @param bizKey        业务幂等键（消息 Key，无则用 msgId）
     * @param tags          消息标签
     * @param body          消息体（JSON），失败时凭此原样入重试表供重放
     * @param callback      真正的业务处理逻辑
     * @return 处理结果
     */
    public ConsumeResult consume(String topic, String consumerGroup, String msgId,
                                 String bizKey, String tags, String body, MessageCallback callback) {
        // 1) 组装一条待落库的记录（初始状态 CONSUMING）
        MqConsumeRecord record = new MqConsumeRecord();
        record.setBizKey(bizKey);
        record.setTopic(topic);
        record.setMsgId(msgId);
        record.setTags(tags);
        record.setConsumerGroup(consumerGroup);
        record.setBody(body);
        record.setStatus(ConsumeStatus.CONSUMING);
        record.setRetryCount(0);
        record.setMaxRetry(maxRetry);

        // 2) 幂等落库（同步）：依赖 (bizKey, topic, consumerGroup) 唯一索引
        boolean firstTime = tryCreateRecord(record);
        if (!firstTime) {
            MqConsumeRecord exist = recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
            if (exist != null && exist.getStatus() == ConsumeStatus.SUCCESS) {
                // 本组已消费成功 → 幂等跳过，绝不重复执行业务
                log.warn("[可靠消息-幂等] 消息重复投递且本组已消费成功，跳过. topic={}, group={}, bizKey={}, msgId={}",
                        topic, consumerGroup, bizKey, msgId);
                return ConsumeResult.DUPLICATE_SKIPPED;
            }
            // 已存在但未成功（失败重投 / 并发插入）：复用原记录继续处理
            record = exist;
            log.info("[可靠消息] 检测到本组未完成的既有记录，复用并重放. topic={}, group={}, bizKey={}, status={}",
                    topic, consumerGroup, bizKey, exist == null ? "N/A" : exist.getStatus());
        }

        if (record == null) {
            // 理论上不会发生（插入失败又查不到），兜底直接执行业务、不落库
            log.warn("[可靠消息] 无法定位消费记录，跳过落库仅执行业务. topic={}, bizKey={}", topic, bizKey);
            try {
                callback.onMessage(body);
                return ConsumeResult.SUCCESS;
            } catch (Exception e) {
                log.error("[可靠消息] 业务异常且无记录可落库. topic={}, bizKey={}, err={}", topic, bizKey, e.getMessage());
                return ConsumeResult.FAILED_SCHEDULED_RETRY;
            }
        }

        final Long recordId = record.getId();
        // 3) 执行业务
        try {
            callback.onMessage(body);
            // 成功：异步回写 SUCCESS，消费线程尽快返回（不阻塞 ack）
            markSuccessAsync(recordId);
            log.info("[可靠消息-成功] 业务处理完成. topic={}, group={}, bizKey={}", topic, consumerGroup, bizKey);
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            // 4) 失败（同步）：置 FAILED + 原始报文写入统一重试表，交由数据库重试接管
            markFailedSync(recordId, e);
            messageRetryService.saveConsumeRetry(bizKey, topic, tags, msgId, consumerGroup,
                    body, recordId, e);
            log.error("[可靠消息-失败] 业务异常，已置 FAILED 并写入重试表等待数据库重试. topic={}, group={}, bizKey={}, 原因={}",
                    topic, consumerGroup, bizKey, e.getMessage());
            return ConsumeResult.FAILED_SCHEDULED_RETRY;
        }
    }

    /* ==================================================================
     * 轻量级消费状态记录（供 broker 原生重试演示 RetryConsumer 使用）
     * ------------------------------------------------------------------
     * 与 consume() 的区别：这些方法不接管重试（重试由 broker 负责），仅按
     * (bizKey, topic, consumerGroup) upsert 一条消费状态记录，便于观测与审计。
     * ================================================================== */

    /**
     * 记录「开始消费」：按 (bizKey, topic, group) upsert 为 CONSUMING。
     *
     * @return 消费记录 ID
     */
    public Long recordConsuming(String topic, String consumerGroup, String msgId,
                                String bizKey, String tags, String body) {
        MqConsumeRecord record = recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
        if (record == null) {
            record = new MqConsumeRecord();
            record.setBizKey(bizKey);
            record.setTopic(topic);
            record.setConsumerGroup(consumerGroup);
            record.setRetryCount(0);
            record.setMaxRetry(maxRetry);
        }
        record.setMsgId(msgId);
        record.setTags(tags);
        record.setBody(body);
        record.setStatus(ConsumeStatus.CONSUMING);
        try {
            recordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            log.warn("[消费记录-落库] 幂等冲突，忽略. topic={}, group={}, bizKey={}", topic, consumerGroup, bizKey);
        }
        return record.getId();
    }

    /** 记录「消费成功」：置 SUCCESS、清空错误与重试时间（同步） */
    public void recordSuccess(String topic, String consumerGroup, String bizKey) {
        MqConsumeRecord record = recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
        if (record == null) {
            return;
        }
        record.setStatus(ConsumeStatus.SUCCESS);
        record.setErrorMsg(null);
        record.setNextRetryTime(null);
        recordRepository.save(record);
    }

    /** 记录「消费失败」：置 FAILED、写错误信息（同步）。broker 会继续重投，故不入重试表 */
    public void recordFailed(String topic, String consumerGroup, String bizKey, Throwable error, Integer retryCount) {
        MqConsumeRecord record = recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
        if (record == null) {
            return;
        }
        record.setStatus(ConsumeStatus.FAILED);
        record.setErrorMsg(truncate(error == null ? null : error.getMessage()));
        if (retryCount != null) {
            record.setRetryCount(retryCount);
        }
        recordRepository.save(record);
    }

    /** 记录「消费死信」：置 DEAD（同步），需人工介入 */
    public void recordDead(String topic, String consumerGroup, String bizKey, Throwable error, Integer retryCount) {
        MqConsumeRecord record = recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
        if (record == null) {
            return;
        }
        record.setStatus(ConsumeStatus.DEAD);
        record.setErrorMsg(truncate(error == null ? null : error.getMessage()));
        if (retryCount != null) {
            record.setRetryCount(retryCount);
        }
        recordRepository.save(record);
        log.error("[消费记录-死信] topic={}, group={}, bizKey={}, retryCount={}", topic, consumerGroup, bizKey, retryCount);
    }

    /* ==================================================================
     * 内部工具
     * ================================================================== */

    /**
     * 尝试插入记录（幂等去重的关键）。
     *
     * @return true=首次插入成功；false=已存在（重复消息）
     */
    private boolean tryCreateRecord(MqConsumeRecord record) {
        try {
            recordRepository.saveAndFlush(record);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    /** 异步回写 SUCCESS：不阻塞消费线程 */
    private void markSuccessAsync(Long recordId) {
        asyncRecordExecutor.submit(() -> {
            MqConsumeRecord record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return;
            }
            record.setStatus(ConsumeStatus.SUCCESS);
            record.setErrorMsg(null);
            record.setNextRetryTime(null);
            recordRepository.save(record);
        });
    }

    /** 同步置 FAILED：累加重试次数、写错误信息，供监控观测（真正的重试排期在 mq_message_retry） */
    private void markFailedSync(Long recordId, Exception e) {
        MqConsumeRecord record = recordRepository.findById(recordId).orElse(null);
        if (record == null) {
            return;
        }
        int retryCount = record.getRetryCount() == null ? 0 : record.getRetryCount();
        record.setRetryCount(retryCount + 1);
        record.setStatus(ConsumeStatus.FAILED);
        record.setErrorMsg(truncate(e.getMessage()));
        recordRepository.save(record);
    }

    /** 错误信息截断，避免超出列长度 */
    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }

    /* ==================== 监控查询（供 REST 接口使用） ==================== */

    /** 查询最近的消费记录 */
    public List<MqConsumeRecord> listRecent() {
        return recordRepository.findTop100ByOrderByCreatedTimeDesc();
    }

    /** 按业务键 + topic + 消费组查询单条记录 */
    public MqConsumeRecord findByBizKey(String bizKey, String topic, String consumerGroup) {
        return recordRepository.findByBizKeyAndTopicAndConsumerGroup(bizKey, topic, consumerGroup);
    }

    /** 统计各状态数量，便于观察失败/死信堆积 */
    public long countByStatus(ConsumeStatus status) {
        return recordRepository.countByStatus(status);
    }
}
