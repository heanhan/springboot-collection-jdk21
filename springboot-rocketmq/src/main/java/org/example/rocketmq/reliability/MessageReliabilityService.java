package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.example.rocketmq.repository.MqConsumeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息可靠性服务（本地消息表模式的核心）。
 *
 * <p><b>用途</b>：为任意消费者提供「消息落库 + 幂等去重 + 失败记录 + 数据库重试」的统一能力。
 * 消费者只需在 {@code onMessage} 中调用 {@link #consume} 并把业务逻辑作为 {@link MessageCallback} 传入，
 * 即可获得完整的可靠性保障，无需自己写落库和重试代码。</p>
 *
 * <p><b>与 broker 重试的区别</b>：RocketMQ 自带的重试是「消费端抛异常 → broker 重投」，
 * 重试次数/间隔受限且不落库、难以查询与人工干预。本服务采用「本地消息表」：
 * 消费失败时<b>不</b>把异常抛给 broker（即向 broker ack），而是记录到数据库，
 * 由 {@link FailedMessageRetryScheduler} 定时重放。这样重试策略完全可控、可观测、可持久。</p>
 *
 * <p><b>幂等设计</b>：{@code (bizKey, topic)} 唯一索引。首次消费插入记录成功才执行业务；
 * 若插入冲突说明消息重复投递，进一步查状态：已 SUCCESS 则直接跳过（幂等），
 * 否则复用原记录继续处理（应对失败重投/并发）。</p>
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
        /** 业务失败，已记录并安排数据库重试 */
        FAILED_SCHEDULED_RETRY,
        /** 重复消息，已幂等跳过 */
        DUPLICATE_SKIPPED
    }

    @Resource
    private MqConsumeRecordRepository recordRepository;

    /** 最大重试次数，来自 application.yml: reliability.max-retry */
    @Value("${reliability.max-retry:3}")
    private int maxRetry;

    /** 重试基础间隔（秒），递增退避：第 n 次重试延迟 = base * n */
    @Value("${reliability.base-retry-interval-seconds:10}")
    private int baseRetryIntervalSeconds;

    /**
     * 可靠消费主流程：幂等落库 → 执行业务 → 记录成功/失败。
     *
     * <p><b>注意</b>：本方法<b>不</b>加 {@code @Transactional}。因为需要在捕获业务异常后，
     * 仍能独立地把「失败记录」写入数据库；若整体包在一个事务里，异常会导致回滚、失败记录也写不进去。</p>
     *
     * @param topic         消息主题
     * @param consumerGroup 消费组
     * @param msgId         RocketMQ 消息 ID
     * @param bizKey        业务幂等键（消息 Key，无则用 msgId）
     * @param tags          消息标签
     * @param body          消息体（JSON）
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

        // 2) 幂等落库：依赖 (bizKey, topic) 唯一索引，重复消息插入会抛 DataIntegrityViolationException
        boolean firstTime = tryCreateRecord(record);
        if (!firstTime) {
            MqConsumeRecord exist = recordRepository.findByBizKeyAndTopic(bizKey, topic);
            if (exist != null && exist.getStatus() == ConsumeStatus.SUCCESS) {
                // 已消费成功 → 幂等跳过，绝不重复执行业务
                log.warn("[可靠消息-幂等] 消息重复投递且已消费成功，跳过. topic={}, bizKey={}, msgId={}",
                        topic, bizKey, msgId);
                return ConsumeResult.DUPLICATE_SKIPPED;
            }
            // 已存在但未成功（失败重投 / 并发插入）：复用原记录继续处理
            record = exist;
            log.info("[可靠消息] 检测到未完成的既有记录，复用并重放. topic={}, bizKey={}, status={}",
                    topic, bizKey, exist == null ? "N/A" : exist.getStatus());
        }

        final Long recordId = record.getId();
        // 3) 执行业务
        try {
            callback.onMessage(body);
            markSuccess(recordId);
            log.info("[可靠消息-成功] 业务处理完成. topic={}, bizKey={}", topic, bizKey);
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            // 4) 失败：记录到数据库并安排重试（不抛异常给 broker，改由本地消息表接管重试）
            ConsumeStatus statusAfter = markFailed(recordId, e);
            log.error("[可靠消息-失败] 业务处理异常，已落库等待数据库重试. topic={}, bizKey={}, 结果状态={}, 原因={}",
                    topic, bizKey, statusAfter, e.getMessage());
            return ConsumeResult.FAILED_SCHEDULED_RETRY;
        }
    }

    /**
     * 数据库重试单条记录：由定时调度器调用。
     *
     * @param record  待重试记录
     * @param handler 对应 topic 的业务处理器
     */
    public void retryOne(MqConsumeRecord record, ReliableMessageHandler handler) {
        try {
            handler.handle(record.getBody());
            markSuccess(record.getId());
            log.info("[可靠消息-重试成功] topic={}, bizKey={}, 第 {} 次重试后成功",
                    record.getTopic(), record.getBizKey(), record.getRetryCount() + 1);
        } catch (Exception e) {
            ConsumeStatus statusAfter = markFailed(record.getId(), e);
            log.warn("[可靠消息-重试失败] topic={}, bizKey={}, 结果状态={}, 原因={}",
                    record.getTopic(), record.getBizKey(), statusAfter, e.getMessage());
        }
    }

    /**
     * 尝试插入记录（幂等去重的关键）。
     *
     * <p>{@code saveAndFlush} 是 Spring Data JPA 自带的事务方法，独立提交；
     * 唯一索引冲突时抛 {@link DataIntegrityViolationException}，此处捕获并返回 false。</p>
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

    /** 标记成功：清空错误信息与重试时间，置为终态 SUCCESS */
    private void markSuccess(Long recordId) {
        MqConsumeRecord record = recordRepository.findById(recordId).orElse(null);
        if (record == null) {
            return;
        }
        record.setStatus(ConsumeStatus.SUCCESS);
        record.setErrorMsg(null);
        record.setNextRetryTime(null);
        recordRepository.save(record);
    }

    /**
     * 标记失败：累加重试次数、按递增退避计算下次重试时间；达上限则转死信。
     *
     * @return 更新后的状态（FAILED 或 DEAD）
     */
    private ConsumeStatus markFailed(Long recordId, Exception e) {
        MqConsumeRecord record = recordRepository.findById(recordId).orElse(null);
        if (record == null) {
            return ConsumeStatus.FAILED;
        }
        int retryCount = record.getRetryCount() == null ? 0 : record.getRetryCount();
        retryCount++;
        record.setRetryCount(retryCount);
        record.setErrorMsg(truncate(e.getMessage()));

        int max = record.getMaxRetry() == null ? maxRetry : record.getMaxRetry();
        if (retryCount >= max) {
            // 达到最大重试次数 → 死信，停止自动重试
            record.setStatus(ConsumeStatus.DEAD);
            record.setNextRetryTime(null);
            log.error("[可靠消息-死信] 重试 {} 次仍失败，转为死信需人工处理. bizKey={}, errorMsg={}",
                    retryCount, record.getBizKey(), record.getErrorMsg());
        } else {
            // 递增退避：第 n 次重试延迟 = base * n 秒
            record.setStatus(ConsumeStatus.FAILED);
            record.setNextRetryTime(LocalDateTime.now().plusSeconds((long) baseRetryIntervalSeconds * retryCount));
        }
        recordRepository.save(record);
        return record.getStatus();
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

    /** 按业务键查询单条记录 */
    public MqConsumeRecord findByBizKey(String bizKey, String topic) {
        return recordRepository.findByBizKeyAndTopic(bizKey, topic);
    }

    /** 统计各状态数量，便于观察失败/死信堆积 */
    public long countByStatus(ConsumeStatus status) {
        return recordRepository.countByStatus(status);
    }
}
