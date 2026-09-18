package org.example.rocketmq.transaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事务消息 - 本地事务监听器。
 *
 * <p><b>用途</b>：实现分布式事务消息的“两阶段提交”。当生产者调用
 * {@code rocketMqTemplate.sendMessageInTransaction(...)} 发送半消息后，RocketMQ 会回调本监听器：</p>
 * <ol>
 *     <li>{@link #executeLocalTransaction} ：执行本地事务（如写订单库、扣库存），
 *         并返回 COMMIT / ROLLBACK / UNKNOWN 决定半消息的最终命运；</li>
 *     <li>{@link #checkLocalTransaction} ：当 broker 迟迟没收到二次确认（如生产者宕机）时，
 *         会定时“回查”本地事务状态，据此决定提交或回滚。</li>
 * </ol>
 *
 * <p><b>适用业务场景</b>：下单成功后再发送“加积分/发通知/扣减库存”消息，
 * 保证本地数据库操作与消息发送的最终一致性。</p>
 *
 * <p><b>注意</b>：一个 {@code RocketMQTemplate} 只能绑定一个事务监听器，
 * 通过 {@code @RocketMQTransactionListener} 注解自动注册。</p>
 *
 * @author demo
 */
@Slf4j
@Component
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    /**
     * 模拟本地事务执行结果的存储（真实项目应查询数据库订单状态）。
     * key = 事务消息的业务标识(orderId)，value = 是否已成功落库。
     */
    private final ConcurrentHashMap<String, Boolean> localTxResult = new ConcurrentHashMap<>();

    /** 回查次数计数器，用于演示“多次回查后再提交”的策略 */
    private final AtomicInteger checkCount = new AtomicInteger(0);

    /**
     * 执行本地事务。
     *
     * @param msg 已发送的半消息（此时消费者还看不到）
     * @param arg 发送时传入的业务参数（这里是我们传的 orderId）
     * @return 事务状态：COMMIT(提交，消息对消费者可见) / ROLLBACK(回滚，丢弃消息) / UNKNOWN(待定，等待回查)
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderId = arg != null ? arg.toString()
                : String.valueOf(msg.getHeaders().get("orderId"));
        log.info("[事务消息] 开始执行本地事务, orderId={}", orderId);
        try {
            // ==== 这里编写真正的本地事务逻辑，例如：保存订单到数据库 ====
            // orderService.createOrder(order);
            // 为了演示，这里模拟本地事务执行成功
            Object payload = msg.getPayload();
            log.info("[事务消息] 本地事务处理中, payload={}", payload);

            // 模拟本地事务成功，记录结果供回查使用
            localTxResult.put(orderId, Boolean.TRUE);

            // 本地事务成功 -> 提交，半消息转为正式消息，消费者可见
            log.info("[事务消息] 本地事务执行成功, 提交消息, orderId={}", orderId);
            return RocketMQLocalTransactionState.COMMIT;

            // 若本地事务失败，可返回：
            // return RocketMQLocalTransactionState.ROLLBACK;  // 回滚，消息被丢弃
            // 若无法立即确定结果（如异步处理中），可返回：
            // return RocketMQLocalTransactionState.UNKNOWN;   // 等待 broker 回查
        } catch (Exception e) {
            log.error("[事务消息] 本地事务执行异常, 回滚消息, orderId={}", orderId, e);
            localTxResult.put(orderId, Boolean.FALSE);
            // 发生异常 -> 回滚，丢弃半消息
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务状态回查。
     *
     * <p>当 {@link #executeLocalTransaction} 返回 UNKNOWN，或生产者在提交前宕机，
     * broker 会按一定间隔多次调用本方法回查本地事务的真实状态。</p>
     *
     * @param msg 需要回查的事务消息
     * @return 回查得到的事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 从消息中取出业务标识（发送时通过 header 设置的 KEYS / orderId）
        Object keyObj = msg.getHeaders().get("orderId");
        String orderId = keyObj != null ? keyObj.toString() : "UNKNOWN-" + msg.getHeaders().getId();
        int times = checkCount.incrementAndGet();
        log.info("[事务消息-回查] 第 {} 次回查, orderId={}", times, orderId);

        // 真实项目：应查询数据库订单是否已创建成功，据此返回状态
        Boolean success = localTxResult.get(orderId);
        if (Boolean.TRUE.equals(success)) {
            // 本地事务已成功 -> 提交
            return RocketMQLocalTransactionState.COMMIT;
        } else if (Boolean.FALSE.equals(success)) {
            // 本地事务已失败 -> 回滚
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        // 仍查不到结果：回查次数较少时可继续等待，超过阈值则回滚，避免消息长期悬挂
        if (times < 3) {
            log.info("[事务消息-回查] 结果未知, 继续等待下次回查, orderId={}", orderId);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
        log.warn("[事务消息-回查] 回查超过阈值仍无结果, 执行回滚, orderId={}", orderId);
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
