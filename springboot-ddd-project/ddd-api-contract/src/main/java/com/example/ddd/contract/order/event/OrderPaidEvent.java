package com.example.ddd.contract.order.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领域事件：订单已支付 (OrderPaid)。
 *
 * <p><b>发布者：</b>order-service（在收到 payment-service 的 PaymentSuccessEvent 后触发）。</p>
 * <p><b>订阅者：</b>logistics-service（创建发货单）、inventory-service（预占转实扣）。</p>
 *
 * @author ddd-learning
 */
public class OrderPaidEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 用户 ID */
    private String userId;
    /** 实付金额 */
    private BigDecimal payAmount;
    /** 支付渠道 */
    private String paymentChannel;
    /** 支付流水号 */
    private String paymentTradeNo;
    /** 支付时间 */
    private LocalDateTime paidAt;

    public OrderPaidEvent() {
        super();
    }

    public OrderPaidEvent(String orderId, String orderNo, String userId, BigDecimal payAmount,
                          String paymentChannel, String paymentTradeNo, LocalDateTime paidAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.payAmount = payAmount;
        this.paymentChannel = paymentChannel;
        this.paymentTradeNo = paymentTradeNo;
        this.paidAt = paidAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public BigDecimal getPayAmount() { return payAmount; }
    public String getPaymentChannel() { return paymentChannel; }
    public String getPaymentTradeNo() { return paymentTradeNo; }
    public LocalDateTime getPaidAt() { return paidAt; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public void setPaymentTradeNo(String paymentTradeNo) { this.paymentTradeNo = paymentTradeNo; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
