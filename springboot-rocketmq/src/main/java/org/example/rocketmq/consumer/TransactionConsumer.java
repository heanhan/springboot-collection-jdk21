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
 * 消费者示例八：事务消息的消费。
 *
 * <p><b>用途</b>：消费事务消息 Topic。对消费者而言，事务消息与普通消息<b>没有区别</b> ——
 * 只有被生产者“提交(COMMIT)”的消息才会对消费者可见；未提交或回滚的半消息永远不会到达这里。</p>
 *
 * <p><b>关键点</b>：消费端无需感知事务，事务的两阶段提交完全由生产者端的
 * {@code OrderTransactionListener} 负责。消费者只需保证业务<b>幂等</b>。</p>
 *
 * <p><b>适用业务场景</b>：接收“已确保与本地事务一致”的下游消息，如订单创建成功后加积分、发通知。</p>
 *
 * <p><b>测试</b>：调用 {@code GET /rocketmq-demo/producer/transaction}，
 * 本地事务提交后，本消费者才会收到消息并打印日志。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_TRANSACTION,
        consumerGroup = RocketMqConstant.GROUP_TRANSACTION
)
public class TransactionConsumer implements RocketMQListener<OrderMessage> {

    @Resource
    private MessageReliabilityService reliabilityService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(OrderMessage message) {
        String bizKey = message.getOrderId();
        String body = toJson(message);
        // 只有事务提交成功的消息才会到达这里；接收落库 -> 执行业务 -> 成功异步回写 / 失败同步入重试表
        reliabilityService.consume(RocketMqConstant.TOPIC_TRANSACTION, RocketMqConstant.GROUP_TRANSACTION,
                null, bizKey, null, body, b ->
                        // 例如：在此为该订单增加积分、发送站内信等（务必保证幂等）
                        log.info("[事务消费] 收到已提交的事务消息: orderId={}, action={}",
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
