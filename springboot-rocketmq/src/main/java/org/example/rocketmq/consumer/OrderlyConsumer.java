package org.example.rocketmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.springframework.stereotype.Service;

/**
 * 消费者示例二：顺序消费（MessageListenerOrderly）。
 *
 * <p><b>用途</b>：保证同一个队列中的消息按发送顺序被<b>串行</b>消费。
 * 配合生产者的 {@code syncSendOrderly(topic, msg, hashKey)}（相同 hashKey 进入同一队列），
 * 即可实现“同一业务主键”的端到端有序。</p>
 *
 * <p><b>关键点</b>：设置 {@code consumeMode = ConsumeMode.ORDERLY}。
 * 顺序消费时，RocketMQ 会对队列加锁，同一队列同一时刻只有一个线程消费，
 * 因此并发度低于普通并发消费，需权衡吞吐与顺序性。</p>
 *
 * <p><b>适用业务场景</b>：订单状态流转（创建→支付→发货→完成）、账户余额增减、
 * 数据库 binlog 同步等必须严格按序处理的场景。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/order}，
 * 观察日志中 CREATE→PAY→DELIVER→FINISH 严格按序打印。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_ORDER,
        consumerGroup = RocketMqConstant.GROUP_ORDER,
        // 核心：开启顺序消费模式（底层即 MessageListenerOrderly）
        consumeMode = ConsumeMode.ORDERLY
)
public class OrderlyConsumer implements RocketMQListener<OrderMessage> {

    @Resource
    private MessageReliabilityService reliabilityService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(OrderMessage message) {
        // 生产端以 orderId+"-"+action 作为 bizKey，消费端保持一致
        String bizKey = message.getOrderId() + "-" + message.getAction();
        String body = toJson(message);
        // 注意：顺序消费下若业务失败，consume() 会吞掉异常并写入重试表（后续重放可能乱序）；
        // 若业务对严格有序敏感，可改为抛异常依赖 broker 就地重试（阻塞后续）。
        reliabilityService.consume(RocketMqConstant.TOPIC_ORDER, RocketMqConstant.GROUP_ORDER,
                null, bizKey, null, body, b ->
                        // 顺序消费：同一 orderId 的消息会按发送顺序依次进入本方法
                        log.info("[顺序消费] orderId={}, action={}（同一订单严格按序处理）",
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
