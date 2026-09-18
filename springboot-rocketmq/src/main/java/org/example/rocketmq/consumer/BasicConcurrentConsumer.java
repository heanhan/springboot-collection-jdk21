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
 * <p><b>ACK 说明</b>：本消费者把消息交给 {@link MessageReliabilityService#consume} 统一处理——
 * 接收即落库(CONSUMING)、业务成功异步回写 SUCCESS、业务失败同步置 FAILED 并写入统一重试表。
 * {@code consume} 内部<b>吞掉</b>业务异常（等于向 broker ack），改由数据库重试接管，避免与 broker 重试双重触发。</p>
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

    /** 消费端可靠性服务：接收落库 + 消费状态回写 + 失败入统一重试表 */
    @Resource
    private MessageReliabilityService reliabilityService;

    /** 用于把 OrderMessage 序列化为 JSON 存入 mq_consume_record.body（失败重试时凭此重放） */
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(OrderMessage message) {
        // bizKey 与生产端落库时一致（orderId），作为幂等键
        String bizKey = message.getOrderId();
        String body = toJson(message);
        // 交给可靠性服务统一处理：落库(CONSUMING) -> 执行业务 -> 成功异步回写 / 失败同步入重试表
        reliabilityService.consume(RocketMqConstant.TOPIC_BASIC, RocketMqConstant.GROUP_BASIC,
                null, bizKey, null, body, b ->
                        // 这里编写真正的业务处理逻辑
                        log.info("[并发消费] 收到消息: orderId={}, action={}, amount={}",
                                message.getOrderId(), message.getAction(), message.getAmount()));
    }

    /** 序列化为 JSON，失败降级为 toString（不影响主流程） */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
