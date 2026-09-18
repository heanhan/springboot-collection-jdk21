package org.example.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.springframework.stereotype.Service;

/**
 * 消费者示例四：指定 Tag 过滤消费。
 *
 * <p><b>用途</b>：同一个 Topic 下可以有多种业务类型的消息，通过 Tag 打标签，
 * 消费者只订阅自己关心的 Tag，实现消息分流。Tag 过滤在 <b>broker 端</b>完成，
 * 减少无效消息的网络传输，效率高于消费端再过滤。</p>
 *
 * <p><b>关键点</b>：</p>
 * <ul>
 *     <li>{@code selectorType = SelectorType.TAG}：使用 Tag 方式过滤（默认即 TAG）；</li>
 *     <li>{@code selectorExpression = "tagA"}：只订阅 tagA；
 *         如需订阅多个用 {@code "tagA || tagB"}；订阅全部用 {@code "*"}。</li>
 * </ul>
 *
 * <p><b>适用业务场景</b>：一个订单 Topic 下区分“创建/支付/退款”等子类型，
 * 不同下游服务只消费各自关心的类型。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/tagkey}，
 * 该接口会同时发送 tagA 与 tagB 两条消息，本消费者只会收到 tagA 那条。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_TAG,
        consumerGroup = RocketMqConstant.GROUP_TAG,
        // 过滤类型：TAG（还有 SQL92 方式，需 broker 开启 enablePropertyFilter）
        selectorType = SelectorType.TAG,
        // 只订阅 tagA；因此 tagB 的消息不会推送给本消费者
        selectorExpression = RocketMqConstant.TAG_A
)
public class TagFilterConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        // 只有 tagA 的消息会进入这里
        log.info("[Tag过滤消费] 收到 tagA 消息: orderId={}, action={}（tagB 已被 broker 过滤）",
                message.getOrderId(), message.getAction());
    }
}
