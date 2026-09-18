package org.example.rocketmq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.example.rocketmq.common.OrderMessage;
import org.example.rocketmq.common.RocketMqConstant;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产者示例控制器。
 *
 * <p><b>用途</b>：把 RocketMQ 常见的 8 种消息发送方式，分别做成独立的 HTTP 接口，
 * 方便学习者用浏览器或 Postman 逐个触发、单独测试。每触发一次，对应的消费者会自动打印日志。</p>
 *
 * <p><b>核心工具</b>：{@link RocketMQTemplate} 是 rocketmq-spring-boot-starter 提供的发送模板，
 * 封装了同步、异步、单向、顺序、延迟、事务等发送能力；消息体对象会被自动序列化为 JSON。</p>
 *
 * <p><b>适用业务场景</b>：订单创建、支付回调、日志采集、数据同步、分布式事务最终一致性等。</p>
 *
 * <p>接口根路径：{@code /rocketmq-demo/producer}</p>
 *
 * @author demo
 */
@Slf4j
@RestController
@RequestMapping("/producer")
public class ProducerController {

    /** RocketMQ 发送模板：所有生产者操作的统一入口 */
    @Resource
    private RocketMQTemplate rocketMqTemplate;

    /** Jackson 序列化工具：批量发送时需要手动把对象转成 JSON 字节数组 */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 测试导航页：列出所有生产者接口，方便快速点击测试。
     * 访问：GET /rocketmq-demo/producer/index
     */
    @GetMapping("/index")
    public Map<String, String> index() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("1-同步发送", "/rocketmq-demo/producer/sync");
        urls.put("2-异步发送(带回调)", "/rocketmq-demo/producer/async");
        urls.put("3-单向发送(Oneway)", "/rocketmq-demo/producer/oneway");
        urls.put("4-延迟发送(默认级别3=10s)", "/rocketmq-demo/producer/delay");
        urls.put("5-顺序发送(按orderId)", "/rocketmq-demo/producer/order");
        urls.put("6-批量发送(10条)", "/rocketmq-demo/producer/batch");
        urls.put("7-事务发送", "/rocketmq-demo/producer/transaction");
        urls.put("8-带Tag和Key发送", "/rocketmq-demo/producer/tagkey");
        urls.put("附-触发失败重试消费", "/rocketmq-demo/producer/retry");
        urls.put("附-触发广播消费", "/rocketmq-demo/producer/broadcast");
        return urls;
    }

    /* ==================================================================
     * 场景一：普通同步消息发送
     * ------------------------------------------------------------------
     * 用途：发送后阻塞等待 broker 返回 SendResult，可靠性最高，是最常用的方式。
     * 适用：重要业务通知（如订单状态变更），需要确保消息一定发送成功。
     * ================================================================== */
    @GetMapping("/sync")
    public String syncSend() {
        OrderMessage order = OrderMessage.of("SYNC-" + System.currentTimeMillis(), "CREATE");
        // syncSend：同步发送，返回发送结果（包含 msgId、发送状态、落在哪个队列等）
        SendResult sendResult = rocketMqTemplate.syncSend(RocketMqConstant.TOPIC_BASIC, order);
        log.info("[同步发送] 结果: status={}, msgId={}", sendResult.getSendStatus(), sendResult.getMsgId());
        return "同步发送成功: " + sendResult.getSendStatus() + ", msgId=" + sendResult.getMsgId();
    }

    /* ==================================================================
     * 场景二：异步消息发送（带回调）
     * ------------------------------------------------------------------
     * 用途：发送后不阻塞，通过 SendCallback 回调处理成功/失败，吞吐量更高。
     * 适用：对响应时间敏感、又不能丢失结果的场景（如实时推送、监控上报）。
     * ================================================================== */
    @GetMapping("/async")
    public String asyncSend() {
        OrderMessage order = OrderMessage.of("ASYNC-" + System.currentTimeMillis(), "PAY");
        // asyncSend：异步发送，第三个参数是回调；方法本身会立即返回，不等待 broker 结果
        rocketMqTemplate.asyncSend(RocketMqConstant.TOPIC_BASIC, order, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                // 发送成功回调：在 RocketMQ 客户端的回调线程中执行
                log.info("[异步发送-成功] msgId={}, status={}", sendResult.getMsgId(), sendResult.getSendStatus());
            }

            @Override
            public void onException(Throwable e) {
                // 发送失败回调：通常在此记录日志、落库补偿或告警
                log.error("[异步发送-失败] 订单={}, 原因={}", order.getOrderId(), e.getMessage(), e);
            }
        });
        return "异步发送已提交（结果见控制台回调日志）";
    }

    /* ==================================================================
     * 场景三：单向发送（Oneway）
     * ------------------------------------------------------------------
     * 用途：只负责发出去，不关心是否成功、无返回结果，性能最高。
     * 适用：日志采集、埋点等允许极少量丢失、追求吞吐的场景。
     * ================================================================== */
    @GetMapping("/oneway")
    public String onewaySend() {
        OrderMessage order = OrderMessage.of("ONEWAY-" + System.currentTimeMillis(), "LOG");
        // sendOneWay：单向发送，无返回值，不等待 broker 响应（注意方法名 W 大写）
        rocketMqTemplate.sendOneWay(RocketMqConstant.TOPIC_BASIC, order);
        log.info("[单向发送] 已发送，orderId={}", order.getOrderId());
        return "单向发送完成（无返回结果，见消费者日志）";
    }

    /* ==================================================================
     * 场景四：延迟消息发送（RocketMQ 内置延迟级别）
     * ------------------------------------------------------------------
     * 用途：消息发送后，延迟指定时间才对消费者可见。
     * RocketMQ 4.x 不支持任意时间延迟，只支持 18 个固定级别：
     *   1=1s 2=5s 3=10s 4=30s 5=1m 6=2m 7=3m 8=4m 9=5m 10=6m
     *   11=7m 12=8m 13=9m 14=10m 15=20m 16=30m 17=1h 18=2h
     * 适用：订单 30 分钟未支付自动取消、延迟提醒、定时任务触发等。
     * ================================================================== */
    @GetMapping("/delay")
    public String delaySend(@RequestParam(defaultValue = "3") int delayLevel) {
        OrderMessage order = OrderMessage.of("DELAY-" + System.currentTimeMillis(), "TIMEOUT_CHECK");
        // 延迟发送需使用 Message 重载：syncSend(destination, Message, timeout毫秒, delayLevel)
        // 这里默认 delayLevel=3，即延迟 10 秒后消费者才能收到
        org.springframework.messaging.Message<OrderMessage> message =
                MessageBuilder.withPayload(order).build();
        SendResult sendResult = rocketMqTemplate.syncSend(
                RocketMqConstant.TOPIC_DELAY, message, 3000, delayLevel);
        log.info("[延迟发送] delayLevel={} (级别3=10s), msgId={}, 发送时刻={}",
                delayLevel, sendResult.getMsgId(), order.getCreateTime());
        return "延迟发送成功: delayLevel=" + delayLevel + "（级别3=10秒后消费），msgId=" + sendResult.getMsgId();
    }

    /* ==================================================================
     * 场景五：顺序消息发送（按业务 key 保证顺序）
     * ------------------------------------------------------------------
     * 用途：相同 hashKey（此处为 orderId）的消息会被投递到同一个队列，
     *       从而保证“同一订单”的消息严格有序（分区有序）。
     * 适用：订单“创建→支付→发货→完成”、账户余额变更等必须有序的场景。
     * 注意：需配合顺序消费者（ConsumeMode.ORDERLY）才能达到端到端有序。
     * ================================================================== */
    @GetMapping("/order")
    public String orderSend(@RequestParam(defaultValue = "ORDER-1001") String orderId) {
        // 模拟同一订单的多个有序动作
        String[] actions = {"CREATE", "PAY", "DELIVER", "FINISH"};
        StringBuilder sb = new StringBuilder();
        for (String action : actions) {
            OrderMessage order = new OrderMessage(orderId, "user-" + orderId, action,
                    new java.math.BigDecimal("199.00"), System.currentTimeMillis());
            // syncSendOrderly(destination, payload, hashKey)：以 orderId 作为分区键保证顺序
            SendResult sendResult = rocketMqTemplate.syncSendOrderly(
                    RocketMqConstant.TOPIC_ORDER, order, orderId);
            sb.append(action).append("->").append(sendResult.getMessageQueue().getQueueId()).append("; ");
            log.info("[顺序发送] orderId={}, action={}, 队列={}", orderId, action,
                    sendResult.getMessageQueue().getQueueId());
        }
        return "顺序发送成功（同一订单进入同一队列）: " + sb;
    }

    /* ==================================================================
     * 场景六：批量消息发送
     * ------------------------------------------------------------------
     * 用途：把多条消息打包成一次网络请求发送，显著提升吞吐、降低网络开销。
     * 限制：同一批消息必须属于同一个 Topic；不能是延迟消息；总大小默认不超过 4MB。
     * 适用：批量数据同步、导入、日志聚合上报等。
     * ================================================================== */
    @GetMapping("/batch")
    public String batchSend(@RequestParam(defaultValue = "10") int count) throws Exception {
        // RocketMQTemplate 未直接提供批量 API，这里取出底层 DefaultMQProducer 手动批量发送
        List<Message> messageList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OrderMessage order = OrderMessage.of("BATCH-" + System.currentTimeMillis() + "-" + i, "BATCH");
            // 手动将对象序列化为 JSON 字节数组，构建成 RocketMQ 原生 Message
            byte[] body = objectMapper.writeValueAsString(order).getBytes(StandardCharsets.UTF_8);
            // Message(topic, tags, keys, body)
            Message message = new Message(RocketMqConstant.TOPIC_BATCH, RocketMqConstant.TAG_A,
                    "batchKey-" + i, body);
            messageList.add(message);
        }
        // 一次性发送整批消息
        SendResult sendResult = rocketMqTemplate.getProducer().send(messageList);
        log.info("[批量发送] 共 {} 条, status={}, msgId={}", count, sendResult.getSendStatus(), sendResult.getMsgId());
        return "批量发送成功: " + count + " 条, msgId=" + sendResult.getMsgId();
    }

    /* ==================================================================
     * 场景七：事务消息发送
     * ------------------------------------------------------------------
     * 用途：保证“本地事务（如扣减库存/写订单库）”与“发送消息”的最终一致性。
     * 流程：
     *   1) 发送“半消息(half msg)”到 broker，此时消费者不可见；
     *   2) 执行本地事务（见 OrderTransactionListener#executeLocalTransaction）；
     *   3) 根据本地事务结果提交(COMMIT)或回滚(ROLLBACK)半消息；
     *   4) 若长时间未确认，broker 会回查(checkLocalTransaction)。
     * 适用：分布式事务，如“下单成功后再发积分/通知消息”。
     * ================================================================== */
    @GetMapping("/transaction")
    public String transactionSend() {
        OrderMessage order = OrderMessage.of("TX-" + System.currentTimeMillis(), "TX_CREATE");
        // 使用 Spring Messaging 的 MessageBuilder 构建消息，可携带业务参数(arg)传给本地事务方法
        org.springframework.messaging.Message<OrderMessage> message = MessageBuilder
                .withPayload(order)
                // 通过 header 传递业务标识，事务监听器/回查时可读取
                .setHeader(RocketMQHeaders.KEYS, order.getOrderId())
                .setHeader("orderId", order.getOrderId())
                .build();
        // sendMessageInTransaction(destination, message, arg)：发送事务消息
        // arg 会作为参数传入 executeLocalTransaction 的第二个参数
        TransactionSendResult result = rocketMqTemplate.sendMessageInTransaction(
                RocketMqConstant.TOPIC_TRANSACTION, message, order.getOrderId());
        log.info("[事务发送] 半消息发送结果: localTxState={}, msgId={}",
                result.getLocalTransactionState(), result.getMsgId());
        return "事务消息已发送: localTxState=" + result.getLocalTransactionState()
                + ", msgId=" + result.getMsgId() + "（本地事务与回查逻辑见 OrderTransactionListener）";
    }

    /* ==================================================================
     * 场景八：发送带 Tag 和 Key 的消息
     * ------------------------------------------------------------------
     * 用途：
     *   - Tag：消息的“子分类”，消费端可按 Tag 过滤订阅（如只消费 tagA）。
     *   - Key：消息的业务主键，便于在 RocketMQ Dashboard 按 Key 精确查询消息、排查问题。
     * 语法：destination 使用 "topic:tag" 形式；Key 通过 header(RocketMQHeaders.KEYS) 设置。
     * 适用：同一 Topic 下多业务类型分流、消息轨迹追踪。
     * ================================================================== */
    @GetMapping("/tagkey")
    public String tagKeySend() {
        String orderId = "TAGKEY-" + System.currentTimeMillis();
        OrderMessage order = OrderMessage.of(orderId, "TAG_TEST");
        // 构建带 Key 的消息；destination 使用 "topic:tag" 指定 Tag
        org.springframework.messaging.Message<OrderMessage> message = MessageBuilder
                .withPayload(order)
                .setHeader(RocketMQHeaders.KEYS, orderId) // 设置消息 Key，可在控制台按 Key 查询
                .build();
        // 发送到 TOPIC_TAG 的 tagA 标签；Tag 过滤消费者只订阅 tagA，因此能收到
        SendResult tagAResult = rocketMqTemplate.syncSend(
                RocketMqConstant.TOPIC_TAG + ":" + RocketMqConstant.TAG_A, message);

        // 再发一条 tagB 的消息作对比：Tag 过滤消费者(只订阅 tagA)将收不到这条
        org.springframework.messaging.Message<OrderMessage> messageB = MessageBuilder
                .withPayload(OrderMessage.of(orderId + "-B", "TAG_TEST_B"))
                .setHeader(RocketMQHeaders.KEYS, orderId + "-B")
                .build();
        SendResult tagBResult = rocketMqTemplate.syncSend(
                RocketMqConstant.TOPIC_TAG + ":" + RocketMqConstant.TAG_B, messageB);

        log.info("[Tag&Key发送] tagA msgId={}, tagB msgId={}", tagAResult.getMsgId(), tagBResult.getMsgId());
        return "已发送 tagA(消费者可收到) 与 tagB(被过滤，收不到)；可在 Dashboard 按 Key=" + orderId + " 查询";
    }

    /* ==================================================================
     * 附加场景 A：触发“失败重试”消费演示
     * 说明：向重试 Topic 发送一条消息，RetryConsumer 收到后会主动抛异常触发重试。
     * ================================================================== */
    @GetMapping("/retry")
    public String retrySend() {
        OrderMessage order = OrderMessage.of("RETRY-" + System.currentTimeMillis(), "RETRY_TEST");
        SendResult sendResult = rocketMqTemplate.syncSend(RocketMqConstant.TOPIC_RETRY, order);
        log.info("[重试演示-发送] msgId={}", sendResult.getMsgId());
        return "已发送到重试 Topic，观察 RetryConsumer 的重试日志（默认最多重试 16 次）";
    }

    /* ==================================================================
     * 附加场景 B：触发“广播消费”演示
     * 说明：向广播 Topic 发送一条消息，BroadcastConsumer（BROADCASTING 模式）会收到。
     * ================================================================== */
    @GetMapping("/broadcast")
    public String broadcastSend() {
        OrderMessage order = OrderMessage.of("BC-" + System.currentTimeMillis(), "BROADCAST");
        SendResult sendResult = rocketMqTemplate.syncSend(RocketMqConstant.TOPIC_BROADCAST, order);
        log.info("[广播演示-发送] msgId={}", sendResult.getMsgId());
        return "已发送到广播 Topic，广播模式下每个消费者实例都会收到该消息";
    }
}
