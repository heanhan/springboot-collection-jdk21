package org.example.rocketmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.springframework.stereotype.Service;

/**
 * 消费者示例七：批量消息的消费。
 *
 * <p><b>用途</b>：消费生产者批量发送的消息。</p>
 *
 * <p><b>重要说明</b>：RocketMQ 的“批量”只是<b>生产者端</b>的发送优化
 * （多条消息合并为一次网络请求）。到达 broker 后它们仍是<b>独立</b>的消息，
 * 因此消费端默认还是<b>逐条</b>回调 {@code onMessage}。</p>
 *
 * <p>如果确实希望消费端“一次拿到多条”批量处理，可以：</p>
 * <ul>
 *     <li>实现 {@code RocketMQListener<java.util.List<MessageExt>>}，并配合
 *         {@code consumeMessageBatchMaxSize} 参数（通过生命周期监听器设置，见 LifecycleManualAckConsumer）；</li>
 *     <li>本示例为降低理解成本，仍采用最常见的逐条消费方式。</li>
 * </ul>
 *
 * <p><b>适用业务场景</b>：批量数据导入后的逐条异步处理、聚合上报的拆分消费等。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/batch?count=10}，
 * 观察日志逐条打印 10 条消息。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(topic = RocketMqConstant.TOPIC_BATCH, consumerGroup = RocketMqConstant.GROUP_BATCH)
public class BatchConsumer implements RocketMQListener<OrderMessage> {

    @Resource
    private MessageReliabilityService reliabilityService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(OrderMessage message) {
        String bizKey = message.getOrderId();
        String body = toJson(message);
        // 接收落库 -> 执行业务 -> 成功异步回写 / 失败同步入重试表
        reliabilityService.consume(RocketMqConstant.TOPIC_BATCH, RocketMqConstant.GROUP_BATCH,
                null, bizKey, RocketMqConstant.TAG_A, body, b ->
                        // 批量发送的消息在消费端仍是逐条到达
                        log.info("[批量消费] 逐条收到: orderId={}, action={}",
                                message.getOrderId(), message.getAction()));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
