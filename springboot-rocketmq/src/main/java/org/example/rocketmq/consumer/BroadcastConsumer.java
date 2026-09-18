package org.example.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.springframework.stereotype.Service;

/**
 * 消费者示例三：广播模式消费（BROADCASTING）。
 *
 * <p><b>用途</b>：与集群模式相反，广播模式下<b>同一个消费者组的每一个实例</b>
 * 都会收到并消费全量消息（各消费各的，互不影响）。</p>
 *
 * <p><b>关键点</b>：设置 {@code messageModel = MessageModel.BROADCASTING}。</p>
 * <ul>
 *     <li>集群模式(CLUSTERING)：一条消息在组内只被消费一次 —— 用于负载均衡；</li>
 *     <li>广播模式(BROADCASTING)：一条消息在组内每个实例都消费一次 —— 用于全节点通知。</li>
 * </ul>
 *
 * <p><b>注意</b>：广播模式下 broker 不维护消费进度（位点由消费者本地存储），
 * 消费失败<b>不会重试</b>，因此只适合可容忍丢失的通知类场景。</p>
 *
 * <p><b>适用业务场景</b>：本地缓存刷新、配置变更通知、集群内所有节点都要执行的操作。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/broadcast}；
 * 若启动多个应用实例，每个实例都会打印消费日志。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_BROADCAST,
        consumerGroup = RocketMqConstant.GROUP_BROADCAST,
        // 核心：设置为广播模式
        messageModel = MessageModel.BROADCASTING
)
public class BroadcastConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        // 广播模式：每个消费者实例都会执行到这里
        log.info("[广播消费] 本实例收到消息: orderId={}, action={}",
                message.getOrderId(), message.getAction());
    }
}
