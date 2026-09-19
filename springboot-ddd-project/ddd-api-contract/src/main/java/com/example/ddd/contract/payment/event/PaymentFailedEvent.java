package com.example.ddd.contract.payment.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：支付失败 (PaymentFailed)。
 *
 * <p><b>发布者：</b>payment-service。</p>
 * <p><b>订阅者：</b>order-service（可选择直接关单，或让用户重试）。</p>
 *
 * @author ddd-learning
 */
public class PaymentFailedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderNo;
    private String userId;
    /** 失败原因码：USER_CANCEL / CHANNEL_REJECT / TIMEOUT / SYSTEM_ERROR */
    private String reasonCode;
    private String reasonDesc;

    public PaymentFailedEvent() {
        super();
    }

    public PaymentFailedEvent(String paymentId, String orderId, String orderNo, String userId,
                              String reasonCode, String reasonDesc) {
        super(paymentId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
    }

    public String getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonDesc() { return reasonDesc; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }
}
