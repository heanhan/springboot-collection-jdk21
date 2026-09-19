package org.example.rocketmq.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.rocketmq.entity.MqMessageRetry;
import org.example.rocketmq.entity.MqProduceRecord;
import org.example.rocketmq.enums.ProduceStatus;
import org.example.rocketmq.enums.RetryStatus;
import org.example.rocketmq.enums.RetryType;
import org.example.rocketmq.reliability.MessageRecordService;
import org.example.rocketmq.reliability.MessageRetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产消息与重试记录查询控制器（mq_produce_record + mq_message_retry 表）。
 *
 * <p><b>用途</b>：提供一组 REST 接口，方便观察「生产端全链路」与「统一重试表」：</p>
 * <ul>
 *     <li>生产记录（mq_produce_record）：PENDING/SUCCESS/FAILED；</li>
 *     <li>重试记录（mq_message_retry）：按 PRODUCE/CONSUME 两类区分，含 PENDING/RETRYING/SUCCESS/DEAD；</li>
 *     <li>消费记录（mq_consume_record）的查询见 {@code ReliableController}。</li>
 * </ul>
 *
 * <p>接口根路径：{@code /rocketmq-demo/message-record}</p>
 *
 * @author demo
 */
@Slf4j
@RestController
@RequestMapping("/message-record")
public class MessageRecordController {

    @Resource
    private MessageRecordService messageRecordService;

    /** 统一重试服务：查询重试表记录与统计（区分生产失败 / 消费失败） */
    @Resource
    private MessageRetryService messageRetryService;

    /** 导航：列出本控制器所有接口 */
    @GetMapping("/index")
    public Map<String, String> index() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("查询最近100条生产记录", "/rocketmq-demo/message-record/records");
        urls.put("按Topic查询最近100条", "/rocketmq-demo/message-record/records-by-topic?topic=demo-basic-topic");
        urls.put("按业务键查询单条生产记录", "/rocketmq-demo/message-record/record?bizKey=SYNC-xxx&topic=demo-basic-topic");
        urls.put("生产状态统计", "/rocketmq-demo/message-record/produce-stats");
        urls.put("重试记录(全部)", "/rocketmq-demo/message-record/retry-records");
        urls.put("重试记录(按类型)", "/rocketmq-demo/message-record/retry-records?type=PRODUCE");
        urls.put("重试状态统计", "/rocketmq-demo/message-record/retry-stats");
        urls.put("生产+重试状态总览", "/rocketmq-demo/message-record/stats");
        return urls;
    }

    /** 查询最近 100 条生产记录（含生产状态、错误信息） */
    @GetMapping("/records")
    public List<MqProduceRecord> records() {
        return messageRecordService.listRecent();
    }

    /** 按 Topic 查询最近 100 条生产记录 */
    @GetMapping("/records-by-topic")
    public List<MqProduceRecord> recordsByTopic(@RequestParam String topic) {
        return messageRecordService.listRecentByTopic(topic);
    }

    /** 按业务幂等键 + Topic 查询单条生产记录 */
    @GetMapping("/record")
    public Object record(@RequestParam String bizKey, @RequestParam String topic) {
        MqProduceRecord record = messageRecordService.findByBizKey(bizKey, topic);
        return record != null ? record : "未找到记录: bizKey=" + bizKey + ", topic=" + topic;
    }

    /** 生产状态数量统计：观察 PENDING / FAILED 是否有堆积 */
    @GetMapping("/produce-stats")
    public Map<String, Long> produceStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ProduceStatus status : ProduceStatus.values()) {
            stats.put(status.name(), messageRecordService.countByProduceStatus(status));
        }
        return stats;
    }

    /** 查询最近 100 条重试记录，可按类型过滤（PRODUCE=生产失败重发 / CONSUME=消费失败重放） */
    @GetMapping("/retry-records")
    public List<MqMessageRetry> retryRecords(@RequestParam(required = false) RetryType type) {
        return type == null ? messageRetryService.listRecent() : messageRetryService.listRecentByType(type);
    }

    /**
     * 重试表状态统计：分别统计「生产失败」「消费失败」两类的各状态数量，
     * 便于观察哪一类失败在堆积、有多少已转死信(DEAD)需人工处理。
     */
    @GetMapping("/retry-stats")
    public Map<String, Object> retryStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (RetryType type : RetryType.values()) {
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (RetryStatus status : RetryStatus.values()) {
                byStatus.put(status.name(), messageRetryService.countByTypeAndStatus(type, status));
            }
            result.put(type.name(), byStatus);
        }
        return result;
    }

    /** 状态总览：生产状态 + 重试状态一并返回 */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("produce", produceStats());
        result.put("retry", retryStats());
        return result;
    }
}
