package org.example.rocketmq.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.example.rocketmq.common.RocketMqConstant;
import org.example.rocketmq.reliability.MessageReliabilityService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 消费者示例九：生命周期定制 / 手动控制（对应“手动确认、提交位点”需求）。
 *
 * <p><b>用途</b>：演示如何在 starter 的 Push 消费模型下，对底层
 * {@link DefaultMQPushConsumer} 做精细化定制（消费线程数、单次拉取条数、最大重试次数等），
 * 并说明“位点提交（ack）”的控制方式。</p>
 *
 * <p><b>关于“手动确认/提交位点”的重要说明</b>：</p>
 * <ul>
 *     <li>rocketmq-spring-boot-starter 的 Push 模式采用<b>自动位点管理</b>：
 *         {@code onMessage} 正常返回即等价于返回 {@code CONSUME_SUCCESS}，
 *         由客户端在后台自动提交位点（ack）；抛出异常则等价于 {@code RECONSUME_LATER}，触发重试。</li>
 *     <li>因此在该 starter 中通常<b>无需也无法</b>像原生 API 那样逐条手动 commit offset；
 *         我们通过“是否抛异常”来表达 ack / nack。</li>
 *     <li>若确实需要<b>完全手动</b>控制位点，应改用原生的 {@code DefaultLitePullConsumer}
 *         （手动 {@code poll()} + {@code commitSync()}），这属于更高级用法，不在本 starter 注解模型内。</li>
 * </ul>
 *
 * <p><b>关键点</b>：实现 {@link RocketMQPushConsumerLifecycleListener}，
 * 在 {@code prepareStart} 中拿到即将启动的 consumer 实例并调参。</p>
 *
 * <p><b>适用业务场景</b>：需要调整并发度、限流、控制重试次数，或对接监控埋点的消费者。</p>
 *
 * @author demo
 */
@Slf4j
@Service
@RocketMQMessageListener(
        // 复用基础 Topic，但使用独立的消费者组：这样同一条消息会被“基础并发消费组”和“本组”各消费一次，
        // 直观演示“不同消费者组之间相互独立、各自都能收到全量消息”的特性。
        topic = RocketMqConstant.TOPIC_BASIC,
        consumerGroup = RocketMqConstant.GROUP_LIFECYCLE
)
public class LifecycleManualAckConsumer
        implements RocketMQListener<MessageExt>, RocketMQPushConsumerLifecycleListener {

    @Resource
    private MessageReliabilityService reliabilityService;

    /**
     * 消费者启动前的回调：可在此对底层 DefaultMQPushConsumer 做定制。
     *
     * @param consumer 即将启动的原生 Push 消费者实例
     */
    @Override
    public void prepareStart(DefaultMQPushConsumer consumer) {
        // 设置消费线程池的最小/最大线程数，控制并发度（限流/提速）
        consumer.setConsumeThreadMin(2);
        consumer.setConsumeThreadMax(8);
        // 单次从 broker 拉取的最大消息条数
        consumer.setPullBatchSize(16);
        // 单次投递给监听器的最大消息条数（>1 时可实现消费端批量处理）
        consumer.setConsumeMessageBatchMaxSize(1);
        // 最大重试次数（超过后进入死信队列 %DLQ%消费组）
        consumer.setMaxReconsumeTimes(5);
        log.info("[生命周期定制] 已自定义消费者参数: 线程[{}~{}], pullBatchSize={}, maxReconsumeTimes={}",
                consumer.getConsumeThreadMin(), consumer.getConsumeThreadMax(),
                consumer.getPullBatchSize(), consumer.getMaxReconsumeTimes());
    }

    /**
     * 消费逻辑：这里使用 MessageExt 泛型以获取消息元数据（如位点、重试次数）。
     *
     * <p>位点控制：正常返回 = 消费成功，客户端自动提交位点；
     * 抛异常 = 消费失败，自动重试。无需手动 commit。</p>
     */
    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        // 幂等键：优先用业务 Key，无则退化为 msgId
        String keys = messageExt.getKeys();
        String bizKey = (keys != null && !keys.isBlank()) ? keys : messageExt.getMsgId();
        // 本消费者与 BasicConcurrentConsumer 同订 TOPIC_BASIC 但不同组；
        // 消费记录按 (bizKey, topic, consumerGroup) 幂等，两组各自独立追踪，互不冲突。
        reliabilityService.consume(RocketMqConstant.TOPIC_BASIC, RocketMqConstant.GROUP_LIFECYCLE,
                messageExt.getMsgId(), bizKey, messageExt.getTags(), body, b ->
                        log.info("[生命周期消费] msgId={}, queueId={}, queueOffset={}, body={}",
                                messageExt.getMsgId(),
                                messageExt.getQueueId(),
                                messageExt.getQueueOffset(), // 消息在队列中的位点
                                b));
    }
}
