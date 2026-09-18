package org.example.rocketmq.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.entity.MqConsumeRecord;
import org.example.rocketmq.enums.ConsumeStatus;
import org.example.rocketmq.reliability.FailedMessageRetryScheduler;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可靠消息演示控制器（本地消息表：幂等去重 + 失败记录 + 数据库重试）。
 *
 * <p><b>用途</b>：提供一组 REST 接口，方便逐个触发「可靠消费」的各种场景，
 * 并可从数据库查询落库记录、观察状态流转、手动触发重试。</p>
 *
 * <p>接口根路径：{@code /rocketmq-demo/reliable}</p>
 *
 * <p><b>推荐体验顺序</b>：</p>
 * <ol>
 *     <li>{@code /send} 正常发送 → 查 {@code /records} 看到 SUCCESS；</li>
 *     <li>{@code /send-duplicate} 重复发送同一 orderId → 第二次日志显示「幂等跳过」，DB 仍只有一条；</li>
 *     <li>{@code /fail-once} 首次失败 → 记录变 FAILED → 等定时任务或调 {@code /retry-now} → 变 SUCCESS；</li>
 *     <li>{@code /fail-always} 一直失败 → 多次 {@code /retry-now} 后 retryCount 达上限 → 变 DEAD（死信）。</li>
 * </ol>
 *
 * @author demo
 */
@Slf4j
@RestController
@RequestMapping("/reliable")
public class ReliableController {

    @Resource
    private RocketMQTemplate rocketMqTemplate;

    @Resource
    private MessageReliabilityService reliabilityService;

    @Resource
    private FailedMessageRetryScheduler retryScheduler;

    /** 导航：列出本控制器所有接口 */
    @GetMapping("/index")
    public Map<String, String> index() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("正常发送(可靠消息)", "/rocketmq-demo/reliable/send?orderId=REL-1001&action=NORMAL");
        urls.put("重复发送(演示幂等跳过)", "/rocketmq-demo/reliable/send-duplicate?orderId=REL-DUP-1");
        urls.put("首次失败后重试成功", "/rocketmq-demo/reliable/fail-once?orderId=REL-FO-1");
        urls.put("永久失败直至死信", "/rocketmq-demo/reliable/fail-always?orderId=REL-FA-1");
        urls.put("查询最近消费记录", "/rocketmq-demo/reliable/records");
        urls.put("按业务键查询单条", "/rocketmq-demo/reliable/record?bizKey=REL-1001");
        urls.put("各状态数量统计", "/rocketmq-demo/reliable/stats");
        urls.put("手动触发一轮重试", "/rocketmq-demo/reliable/retry-now");
        return urls;
    }

    /**
     * 发送一条可靠消息（正常成功）。
     * 通过 {@code RocketMQHeaders.KEYS} 设置业务幂等键 = orderId。
     */
    @GetMapping("/send")
    public String send(@RequestParam(defaultValue = "REL-1001") String orderId,
                       @RequestParam(defaultValue = "NORMAL") String action) {
        SendResult result = doSend(orderId, action);
        return "已发送可靠消息: orderId=" + orderId + ", action=" + action
                + ", msgId=" + result.getMsgId() + "。稍后查 /reliable/records 可见 SUCCESS 记录";
    }

    /**
     * 重复发送同一 orderId 两次，演示「幂等去重」：
     * 第二次消费时因 (bizKey, topic) 唯一索引冲突且原记录已 SUCCESS，会被跳过。
     */
    @GetMapping("/send-duplicate")
    public String sendDuplicate(@RequestParam(defaultValue = "REL-DUP-1") String orderId) {
        SendResult first = doSend(orderId, "NORMAL");
        SendResult second = doSend(orderId, "NORMAL");
        return "已用相同 orderId=" + orderId + " 发送两次。第一次 msgId=" + first.getMsgId()
                + "，第二次 msgId=" + second.getMsgId()
                + "。观察日志：第二次应显示「幂等跳过」，且数据库只有 1 条记录";
    }

    /** 发送一条「首次失败、重试后成功」的消息，演示数据库重试 */
    @GetMapping("/fail-once")
    public String failOnce(@RequestParam(defaultValue = "REL-FO-1") String orderId) {
        SendResult result = doSend(orderId, "FAIL_ONCE");
        return "已发送 FAIL_ONCE 消息: orderId=" + orderId + ", msgId=" + result.getMsgId()
                + "。首次消费会失败并落库为 FAILED，等待定时任务重试（或调 /reliable/retry-now）后变 SUCCESS";
    }

    /** 发送一条「永远失败」的消息，演示重试达上限后转死信（DEAD） */
    @GetMapping("/fail-always")
    public String failAlways(@RequestParam(defaultValue = "REL-FA-1") String orderId) {
        SendResult result = doSend(orderId, "FAIL_ALWAYS");
        return "已发送 FAIL_ALWAYS 消息: orderId=" + orderId + ", msgId=" + result.getMsgId()
                + "。多次重试（/reliable/retry-now）达上限后，记录状态将变为 DEAD（死信），需人工处理";
    }

    /** 查询最近的消费记录（含状态、重试次数、错误信息、下次重试时间） */
    @GetMapping("/records")
    public List<MqConsumeRecord> records() {
        return reliabilityService.listRecent();
    }

    /** 按业务幂等键查询单条记录 */
    @GetMapping("/record")
    public Object record(@RequestParam String bizKey,
                         @RequestParam(defaultValue = RocketMqConstant.TOPIC_RELIABLE) String topic) {
        MqConsumeRecord record = reliabilityService.findByBizKey(bizKey, topic);
        return record != null ? record : "未找到记录: bizKey=" + bizKey + ", topic=" + topic;
    }

    /** 各状态数量统计，便于观察 FAILED / DEAD 堆积 */
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ConsumeStatus status : ConsumeStatus.values()) {
            stats.put(status.name(), reliabilityService.countByStatus(status));
        }
        return stats;
    }

    /** 手动触发一轮数据库重试（等价于定时任务立即执行一次） */
    @GetMapping("/retry-now")
    public String retryNow() {
        int handled = retryScheduler.retryNow();
        return "手动重试完成，本轮处理 " + handled + " 条失败消息。可再查 /reliable/records 观察状态变化";
    }

    /**
     * 统一的可靠消息发送：设置 KEYS=orderId 作为幂等键。
     *
     * @param orderId 订单号（作为业务幂等键）
     * @param action  业务动作（NORMAL / FAIL_ONCE / FAIL_ALWAYS）
     * @return 发送结果
     */
    private SendResult doSend(String orderId, String action) {
        OrderMessage order = OrderMessage.of(orderId, action);
        // 关键：设置消息 Key = orderId，消费端以此作为幂等键落库去重
        Message<OrderMessage> message = MessageBuilder.withPayload(order)
                .setHeader(RocketMQHeaders.KEYS, orderId)
                .build();
        SendResult result = rocketMqTemplate.syncSend(RocketMqConstant.TOPIC_RELIABLE, message);
        log.info("[可靠消息-发送] orderId={}, action={}, msgId={}, status={}",
                orderId, action, result.getMsgId(), result.getSendStatus());
        return result;
    }
}
