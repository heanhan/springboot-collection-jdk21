package org.example.rocketmq.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 消费者示例五：消费失败重试与异常处理。
 *
 * <p><b>用途</b>：演示当消费逻辑抛出异常时，RocketMQ 的重试机制如何工作，
 * 以及如何读取“重试次数”做差异化处理（如超过阈值后落库人工干预）。</p>
 *
 * <p><b>重试原理（集群模式 + 并发消费）</b>：</p>
 * <ul>
 *     <li>{@code onMessage} 正常返回 = 消费成功，提交位点；</li>
 *     <li>{@code onMessage} 抛出异常 = 消费失败，消息进入重试队列 {@code %RETRY%消费者组}，
 *         并按递增延迟（10s、30s、1m、2m...）重投，默认最多重试 16 次；</li>
 *     <li>16 次仍失败后进入死信队列 {@code %DLQ%消费者组}，需人工处理。</li>
 * </ul>
 *
 * <p><b>关键点</b>：这里泛型使用 {@code MessageExt} 而非业务对象，
 * 目的是能拿到 {@code getReconsumeTimes()}（已重试次数）、msgId 等元数据。
 * 消息体需自行从 {@code getBody()} 反序列化。</p>
 *
 * <p><b>适用业务场景</b>：调用第三方接口可能临时失败、需要重试兜底的任务；
 * 同时强调消费端务必做<b>幂等</b>，因为重试会导致消息被多次投递。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/retry}，
 * 观察日志中 reconsumeTimes 递增，以及达到阈值后的“死信/人工处理”分支。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_RETRY,
        consumerGroup = RocketMqConstant.GROUP_RETRY
)
public class RetryConsumer implements RocketMQListener<MessageExt> {

    /** 演示用：最大容忍的重试次数，超过则认为无法自动恢复 */
    private static final int MAX_HANDLE_TIMES = 3;

    @Resource
    private MessageReliabilityService reliabilityService;

    @Override
    public void onMessage(MessageExt messageExt) {
        // 获取已重试次数：0 表示首次消费，1 表示第一次重试，以此类推
        int reconsumeTimes = messageExt.getReconsumeTimes();
        // 手动解析消息体（MessageExt 拿到的是原始字节）
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        // 幂等键：优先用业务 Key（发送时通过 RocketMQHeaders.KEYS 设置为 orderId），无则退化为 msgId
        String keys = messageExt.getKeys();
        String bizKey = (keys != null && !keys.isBlank()) ? keys : messageExt.getMsgId();
        log.info("[重试消费] 收到消息: msgId={}, bizKey={}, 已重试次数={}, body={}",
                messageExt.getMsgId(), bizKey, reconsumeTimes, body);

        // 接收即落库(CONSUMING)；本示例重试由 broker 接管，故仅记录状态、不写统一重试表
        reliabilityService.recordConsuming(RocketMqConstant.TOPIC_RETRY, RocketMqConstant.GROUP_RETRY,
                messageExt.getMsgId(), bizKey, messageExt.getTags(), body);
        try {
            // ==== 模拟业务处理：这里故意抛出异常来触发重试 ====
            // 真实场景可能是：调用远程接口超时、数据库死锁、依赖服务不可用等
            if (reconsumeTimes < MAX_HANDLE_TIMES) {
                // 尚未达到容忍上限，抛异常 -> 触发 RocketMQ 重试
                throw new RuntimeException("模拟业务处理失败，等待重试（当前第 " + reconsumeTimes + " 次重试）");
            }

            // 达到重试上限：不再抛异常（否则最终进入死信队列），改为落库/告警人工介入
            log.warn("[重试消费] 已重试 {} 次仍失败, 转为记录并人工处理, 不再重试. msgId={}, body={}",
                    reconsumeTimes, messageExt.getMsgId(), body);
            // 正常返回 = ack，消息不会再被重投（此处已由业务补偿接管）
            // 消费记录置为 DEAD（死信），体现「消费失败达上限」
            reliabilityService.recordDead(RocketMqConstant.TOPIC_RETRY, RocketMqConstant.GROUP_RETRY,
                    bizKey, null, reconsumeTimes);
        } catch (Exception e) {
            // 关键：把异常继续抛出，starter 才会将其判定为消费失败并触发重试
            log.error("[重试消费] 处理异常, 将触发重试: {}", e.getMessage());
            // 回写消费记录为 FAILED（携带当前重试次数）
            reliabilityService.recordFailed(RocketMqConstant.TOPIC_RETRY, RocketMqConstant.GROUP_RETRY,
                    bizKey, e, reconsumeTimes);
            throw new RuntimeException(e);
        }
    }
}
