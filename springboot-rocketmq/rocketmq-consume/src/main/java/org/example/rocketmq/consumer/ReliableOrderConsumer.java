package org.example.rocketmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.example.rocketmq.reliability.ReliableMessageHandler;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消费者示例十：可靠消费（本地消息表 = 幂等去重 + 失败记录 + 数据库重试）。
 *
 * <p><b>用途</b>：演示生产级「消息可靠消费」的完整设计——消息落库 MySQL、
 * 防止重复消费、失败记录、从数据库定时重试、超过阈值转死信。这是本模块相比
 * broker 自带重试（见 {@link RetryConsumer}）更可控、可观测的方案。</p>
 *
 * <p><b>工作机制</b>：</p>
 * <ol>
 *     <li>{@code onMessage} 收到消息后，交给 {@link MessageReliabilityService#consume} 统一处理；</li>
 *     <li>服务先用 {@code (bizKey, topic)} 唯一索引把消息<b>落库</b>：插入冲突即重复消息，
 *         已成功则<b>幂等跳过</b>；</li>
 *     <li>执行 {@link #handle} 业务：成功异步回写 SUCCESS，失败<b>不抛异常给 broker</b>（等于向 broker ack），
 *         而是置 FAILED 并把原始报文写入统一重试表(type=CONSUME)，由 {@code MessageRetryScheduler} 定时<b>重放</b>；</li>
 *     <li>重试达上限仍失败 → 置 DEAD（死信），需人工介入。</li>
 * </ol>
 *
 * <p><b>为什么失败不抛异常</b>：一旦抛异常，broker 也会重试，就会与数据库重试「双重重试」。
 * 本方案选择由数据库接管重试，因此消费方法内吞掉异常、正常返回，保证 broker 侧只投递一次。</p>
 *
 * <p><b>如何触发各分支（见 {@code ReliableController}）</b>：
 * action=NORMAL 正常成功；重复发送同一 orderId 触发幂等跳过；
 * action=FAIL_ONCE 首次失败、重试后成功；action=FAIL_ALWAYS 一直失败直至死信。</p>
 *
 * <p>本类同时实现 {@link ReliableMessageHandler}，把自身注册进处理器表，
 * 使数据库重试时能按 topic 反查到本类并重放 {@link #handle}。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = RocketMqConstant.TOPIC_RELIABLE,
        consumerGroup = RocketMqConstant.GROUP_RELIABLE
)
public class ReliableOrderConsumer implements RocketMQListener<MessageExt>, ReliableMessageHandler {

    @Resource
    private MessageReliabilityService reliabilityService;

    /** 用于手动反序列化消息体（数据库重试时同样要把 JSON 还原为对象） */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 演示「失败一次后重试成功」：记录已经失败过一次的订单号。
     * 真实业务不需要它——这里仅为了让你直观看到「重试后成功」的效果。
     */
    private final Set<String> failedOnceOrders = ConcurrentHashMap.newKeySet();

    @Override
    public void onMessage(MessageExt messageExt) {
        // 消息体（JSON）
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        // 幂等键：优先用业务 Key（发送时通过 RocketMQHeaders.KEYS 设置为 orderId），无则退化为 msgId
        String keys = messageExt.getKeys();
        String bizKey = (keys != null && !keys.isBlank()) ? keys : messageExt.getMsgId();

        log.info("[可靠消费] 收到消息: topic={}, msgId={}, bizKey={}, tags={}, body={}",
                RocketMqConstant.TOPIC_RELIABLE, messageExt.getMsgId(), bizKey, messageExt.getTags(), body);

        // 交给可靠性服务统一处理（落库 + 幂等 + 失败记录）；此方法内部已吞掉业务异常，不会触发 broker 重试
        MessageReliabilityService.ConsumeResult result = reliabilityService.consume(
                RocketMqConstant.TOPIC_RELIABLE,
                RocketMqConstant.GROUP_RELIABLE,
                messageExt.getMsgId(),
                bizKey,
                messageExt.getTags(),
                body,
                this::handle);

        log.info("[可靠消费] 处理结果: bizKey={}, result={}", bizKey, result);
    }

    /** 本处理器负责的 topic，供数据库重试时反查 */
    @Override
    public String supportTopic() {
        return RocketMqConstant.TOPIC_RELIABLE;
    }

    /**
     * 真正的业务逻辑：首次消费与数据库重试都会回调此方法（重放）。
     * 处理成功正常返回；处理失败抛异常，由框架记录失败并安排重试。
     */
    @Override
    public void handle(String body) throws Exception {
        OrderMessage order = objectMapper.readValue(body, OrderMessage.class);
        doBusiness(order);
    }

    /**
     * 模拟业务处理，通过 action 演示不同的可靠性分支。
     *
     * @param order 订单消息
     */
    private void doBusiness(OrderMessage order) {
        String action = order.getAction();
        String orderId = order.getOrderId();

        switch (action == null ? "" : action) {
            case "FAIL_ALWAYS" ->
                // 永远失败：重试到上限后转死信（DEAD），演示死信兜底
                    throw new IllegalStateException("模拟业务永久失败(FAIL_ALWAYS), orderId=" + orderId);
            case "FAIL_ONCE" -> {
                // 首次失败、重试成功：用内存集合记录是否已失败过
                if (failedOnceOrders.add(orderId)) {
                    throw new IllegalStateException("模拟业务首次失败(FAIL_ONCE)，将由数据库重试, orderId=" + orderId);
                }
                log.info("[可靠消费-业务] FAIL_ONCE 重试后成功, orderId={}", orderId);
            }
            default ->
                // 正常业务：这里可写「扣减库存 / 更新订单 / 发通知」等真实逻辑
                    log.info("[可靠消费-业务] 处理成功, orderId={}, action={}, amount={}",
                            orderId, action, order.getAmount());
        }
    }
}
