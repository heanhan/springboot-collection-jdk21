package org.example.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.springframework.stereotype.Service;

/**
 * 消费者示例一：普通并发消费（Push 模式 + 集群模式）。
 *
 * <p><b>用途</b>：这是最常见、最基础的消费方式。broker 主动“推”消息给消费者，
 * 多线程并发处理，吞吐量高。</p>
 *
 * <p><b>关键概念</b>：</p>
 * <ul>
 *     <li><b>消费模式（默认 CLUSTERING 集群）</b>：同一个消费者组内，一条消息只会被其中一个实例消费一次，
 *         适合绝大多数业务（负载均衡）。对应的另一种是 BROADCASTING 广播模式（见 BroadcastConsumer）。</li>
 *     <li><b>消费方式（默认 CONCURRENTLY 并发）</b>：组内多线程并发消费，不保证顺序；
 *         如需保证顺序使用 ORDERLY（见 OrderlyConsumer）。</li>
 *     <li>泛型 {@code RocketMQListener<OrderMessage>}：starter 会自动把 JSON 消息体反序列化为 OrderMessage。</li>
 * </ul>
 *
 * <p><b>适用业务场景</b>：普通的异步解耦任务，如日志处理、非顺序的数据同步、通知下发等。</p>
 *
 * <p><b>ACK 说明</b>：{@link #onMessage} 正常返回即代表消费成功，starter 会自动提交消费位点（ack）；
 * 若抛出异常，则会触发重试（见 RetryConsumer）。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        // 订阅的 Topic
        topic = RocketMqConstant.TOPIC_BASIC,
        // 消费者组：必须全局唯一，同组内消费逻辑要一致
        consumerGroup = RocketMqConstant.GROUP_BASIC
        // consumeMode 默认 CONCURRENTLY（并发）；messageModel 默认 CLUSTERING（集群）
)
public class BasicConcurrentConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        // 这里编写真正的业务处理逻辑
        log.info("[并发消费] 收到消息: orderId={}, action={}, amount={}",
                message.getOrderId(), message.getAction(), message.getAmount());
        // 正常返回 -> 消费成功，自动提交位点；抛异常 -> 进入重试队列
    }
}
