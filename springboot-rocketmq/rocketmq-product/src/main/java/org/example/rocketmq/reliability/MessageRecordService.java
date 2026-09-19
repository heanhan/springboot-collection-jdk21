package org.example.rocketmq.reliability;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.concurrent.AsyncRecordExecutor;
import org.example.rocketmq.entity.MqProduceRecord;
import org.example.rocketmq.enums.ProduceStatus;
import org.example.rocketmq.repository.MqProduceRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产端消息记录服务（mq_produce_record 表的核心）。
 *
 * <p><b>职责边界</b>：本服务<b>只</b>负责生产端——「发送前落库、发送后回写结果、失败入重试表」。
 * 消费端状态见 {@link MessageReliabilityService}（mq_consume_record），失败重试统一见
 * {@link MessageRetryService}（mq_message_retry）。</p>
 *
 * <p><b>关键设计（对应用户「同步/异步不影响性能」的要求）</b>：</p>
 * <ul>
 *     <li><b>发送前落库 PENDING —— 同步</b>：{@link #createPending} 用 {@code saveAndFlush} 独立提交，
 *         保证「先落库、再发送」；即便发送过程中应用宕机，PENDING 记录仍存在，可由补偿任务扫描重发。</li>
 *     <li><b>发送成功回写 SUCCESS —— 异步</b>：{@link #markProduceSuccess} 交给 {@link AsyncRecordExecutor}
 *         的 JUC 线程池执行，主发送链路无需等待 DB 写，显著提升吞吐；即便偶发丢失，记录停留在 PENDING，
 *         可由补偿任务修正，不影响正确性。</li>
 *     <li><b>发送失败置 FAILED + 入重试表 —— 同步</b>：{@link #markProduceFailed} 关乎可靠性，绝不能丢，
 *         因此同步执行，并把<b>原始报文</b>写入统一重试表(type=PRODUCE)，供后续「取原始数据重新生产」。</li>
 *     <li><b>幂等键约束</b>：{@code (bizKey, topic)} 唯一索引；重复调用 {@code createPending}
 *         会捕获 {@link DataIntegrityViolationException} 并返回既有记录 ID，不抛异常。</li>
 * </ul>
 *
 * @author demo
 */
@Slf4j
@Service
public class MessageRecordService {

    @Resource
    private MqProduceRecordRepository produceRecordRepository;

    /** 异步落库执行器：把「发送成功」的状态回写从主链路剥离，交给 JUC 线程池异步执行 */
    @Resource
    private AsyncRecordExecutor asyncRecordExecutor;

    /** 统一重试服务：发送失败时把原始报文写入 mq_message_retry(type=PRODUCE) */
    @Resource
    private MessageRetryService messageRetryService;

    /* ==================================================================
     * 一、发送前落库（同步）
     * ================================================================== */

    /**
     * 发送前落库为 PENDING（同步、独立提交事务）。
     *
     * <p>调用时机：在 {@code rocketMqTemplate.syncSend(...)} 之<b>前</b>。</p>
     *
     * @param topic         消息主题
     * @param tags          消息标签（可空）
     * @param bizKey        业务幂等键（务必与 {@code RocketMQHeaders.KEYS} 一致）
     * @param body          消息体 JSON（生产重试时凭此原样重发）
     * @param producerGroup 生产者组
     * @return 落库记录的主键 ID；若 (bizKey, topic) 已存在则返回既有记录 ID
     */
    public Long createPending(String topic, String tags, String bizKey, String body, String producerGroup) {
        MqProduceRecord record = new MqProduceRecord();
        record.setBizKey(bizKey);
        record.setTopic(topic);
        record.setTags(tags);
        record.setBody(body);
        record.setProducerGroup(producerGroup);
        record.setProduceStatus(ProduceStatus.PENDING);
        try {
            // saveAndFlush：独立事务立即提交，保证 PENDING 记录先于 broker 发送落盘
            produceRecordRepository.saveAndFlush(record);
            log.debug("[生产消息-落库] PENDING, topic={}, bizKey={}, id={}", topic, bizKey, record.getId());
            return record.getId();
        } catch (DataIntegrityViolationException e) {
            // 幂等：同 (bizKey, topic) 已存在，返回既有 ID，交由调用方决定后续处理
            MqProduceRecord exist = produceRecordRepository.findByBizKeyAndTopic(bizKey, topic);
            log.warn("[生产消息-落库] 幂等命中，已存在同 bizKey 记录. topic={}, bizKey={}, existId={}",
                    topic, bizKey, exist == null ? null : exist.getId());
            return exist == null ? null : exist.getId();
        }
    }

    /* ==================================================================
     * 二、发送成功回写（异步，不影响主链路性能）
     * ================================================================== */

    /**
     * 发送成功：异步回填 msgId、置 SUCCESS、记录发送完成时间。
     *
     * <p>走 {@link AsyncRecordExecutor} 线程池，主发送线程立即返回；异步任务内部异常被吞并计数，
     * 绝不影响发送链路。若异步写偶发失败，记录停留 PENDING，可由补偿任务修正。</p>
     *
     * @param recordId 由 {@link #createPending} 返回的 ID
     * @param msgId    broker 返回的消息 ID（oneway 场景可能为 null）
     */
    public void markProduceSuccess(Long recordId, String msgId) {
        if (recordId == null) {
            return;
        }
        asyncRecordExecutor.submit(() -> {
            MqProduceRecord record = produceRecordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return;
            }
            record.setProduceStatus(ProduceStatus.SUCCESS);
            record.setMsgId(msgId);
            record.setProduceTime(LocalDateTime.now());
            record.setProduceError(null);
            produceRecordRepository.save(record);
            log.debug("[生产消息-发送成功] id={}, bizKey={}, msgId={}", recordId, record.getBizKey(), msgId);
        });
    }

    /* ==================================================================
     * 三、发送失败回写（同步）+ 落统一重试表
     * ================================================================== */

    /**
     * 发送失败：同步置 FAILED、记录错误，并把<b>原始报文</b>写入统一重试表(type=PRODUCE)。
     *
     * <p>调用后调用方仍应把异常抛给上层，本方法只负责状态落库与入重试表。
     * 重试表保存了 topic/tags/body/bizKey，后续调度器可「取原始数据重新生产」。</p>
     *
     * @param recordId 由 {@link #createPending} 返回的 ID
     * @param error    失败原因
     */
    public void markProduceFailed(Long recordId, Throwable error) {
        MqProduceRecord record = findById(recordId);
        if (record == null) {
            return;
        }
        record.setProduceStatus(ProduceStatus.FAILED);
        record.setProduceTime(LocalDateTime.now());
        record.setProduceError(truncate(error == null ? null : error.getMessage()));
        produceRecordRepository.save(record);
        log.warn("[生产消息-发送失败] id={}, bizKey={}, error={}",
                recordId, record.getBizKey(), record.getProduceError());

        // 失败入统一重试表：保存原始报文，供后续「生产重试」直接重发
        messageRetryService.saveProduceRetry(
                record.getBizKey(), record.getTopic(), record.getTags(), record.getBody(),
                record.getProducerGroup(), record.getId(), error);
    }

    /* ==================================================================
     * 四、监控查询 API
     * ================================================================== */

    /** 查询最近 100 条生产记录 */
    public List<MqProduceRecord> listRecent() {
        return produceRecordRepository.findTop100ByOrderByCreatedTimeDesc();
    }

    /** 按 Topic 查询最近 100 条生产记录 */
    public List<MqProduceRecord> listRecentByTopic(String topic) {
        return produceRecordRepository.findTop100ByTopicOrderByCreatedTimeDesc(topic);
    }

    /** 按业务幂等键 + Topic 查询单条 */
    public MqProduceRecord findByBizKey(String bizKey, String topic) {
        return produceRecordRepository.findByBizKeyAndTopic(bizKey, topic);
    }

    /** 按生产状态统计 */
    public long countByProduceStatus(ProduceStatus status) {
        return produceRecordRepository.countByProduceStatus(status);
    }

    /* ==================================================================
     * 内部工具
     * ================================================================== */

    private MqProduceRecord findById(Long id) {
        if (id == null) {
            return null;
        }
        return produceRecordRepository.findById(id).orElse(null);
    }

    /** 错误信息截断，避免超出列长度 */
    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}
