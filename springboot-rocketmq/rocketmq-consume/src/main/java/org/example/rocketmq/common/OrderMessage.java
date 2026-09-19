package org.example.rocketmq.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 统一消息体（以“订单”业务为例）。
 *
 * <p><b>用途</b>：作为生产者发送、消费者接收的载体对象。rocketmq-spring-boot-starter
 * 会自动使用 Jackson 将其序列化为 JSON 发送，消费端也能自动反序列化为该对象，
 * 因此无需手动处理 JSON。</p>
 *
 * <p><b>要求</b>：必须实现 {@link Serializable} 并提供无参构造（{@code @NoArgsConstructor}），
 * 以保证反序列化正常。</p>
 *
 * @author demo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号：顺序消息中作为分区键（hashKey），保证同一订单的消息进入同一队列 */
    private String orderId;

    /** 用户 ID */
    private String userId;

    /** 业务动作，如：CREATE(创建)、PAY(支付)、CANCEL(取消) */
    private String action;

    /** 订单金额 */
    private BigDecimal amount;

    /** 消息创建时间戳（毫秒），用于观察延迟消息的实际延迟效果 */
    private long createTime;

    /**
     * 便捷构建方法。
     *
     * @param orderId 订单号
     * @param action  业务动作
     * @return 消息对象（自动填充创建时间）
     */
    public static OrderMessage of(String orderId, String action) {
        return new OrderMessage(orderId, "user-" + orderId, action,
                new BigDecimal("99.90"), System.currentTimeMillis());
    }
}
