package org.example.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.springframework.stereotype.Service;

/**
 * 消费者示例六：延迟消息消费。
 *
 * <p><b>用途</b>：消费生产者发送的“延迟消息”。延迟消息在达到延迟时间后，
 * 才会被投递给消费者，消费者代码本身与普通消费无异 —— 延迟是由 broker 控制的。</p>
 *
 * <p><b>关键点</b>：通过对比消息中的 {@code createTime}（发送时刻）与当前消费时刻，
 * 可以直观看到延迟效果（默认延迟级别 3 = 10 秒）。</p>
 *
 * <p><b>适用业务场景</b>：订单超时未支付自动关闭、预约提醒、延迟重试、定时通知等。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/delay}（可用 ?delayLevel=4 指定级别），
 * 观察日志中“消费时刻”比“发送时刻”晚约对应延迟时间。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_DELAY,
        consumerGroup = RocketMqConstant.GROUP_DELAY
)
public class DelayConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        long now = System.currentTimeMillis();
        // 计算实际延迟时长（消费时刻 - 发送时刻）
        long delayed = now - message.getCreateTime();
        log.info("[延迟消费] orderId={}, 发送时刻={}, 消费时刻={}, 实际延迟≈{}ms",
                message.getOrderId(), message.getCreateTime(), now, delayed);
        // 例如：在此判断订单是否仍未支付，若未支付则执行关单逻辑
    }
}
