package com.example.ddd.contract.payment.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领域事件：支付成功 (PaymentSuccess)。
 *
 * <p><b>发布者：</b>payment-service（渠道回调验签通过后触发）。</p>
 * <p><b>订阅者：</b>order-service（把订单状态改为 PAID，再发 OrderPaidEvent）。</p>
 *
 * <p><b>为什么不直接由 payment-service 发 OrderPaidEvent？</b>
 * 因为"订单已支付"是订单上下文的事实，只有订单聚合根才能改变自己的状态。
 * payment 只能发布"我这边收到了钱"这个事实，由 order 决定是否 / 何时把订单标记为已支付。
 * 这是 DDD 中<b>限界上下文自治</b>的重要体现。</p>
 *
 * @author ddd-learning
 */
public class PaymentSuccessEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderNo;
    private String userId;
    private BigDecimal amount;
    private String channel;
    private String tradeNo;
    private LocalDateTime paidAt;

    public PaymentSuccessEvent() {
        super();
    }

    public PaymentSuccessEvent(String paymentId, String orderId, String orderNo, String userId,
                               BigDecimal amount, String channel, String tradeNo, LocalDateTime paidAt) {
        super(paymentId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
        this.channel = channel;
        this.tradeNo = tradeNo;
        this.paidAt = paidAt;
    }

    public String getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getChannel() { return channel; }
    public String getTradeNo() { return tradeNo; }
    public LocalDateTime getPaidAt() { return paidAt; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
