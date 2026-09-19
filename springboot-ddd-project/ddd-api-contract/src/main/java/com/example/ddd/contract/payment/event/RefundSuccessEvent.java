package com.example.ddd.contract.payment.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;
import java.math.BigDecimal;

/** 支付上下文事实：全额退款已完成；订单消费后推进退款状态。 */
public class RefundSuccessEvent extends AbstractDomainEvent {
    private String orderId;
    private BigDecimal amount;
    public RefundSuccessEvent() { super(); }
    public RefundSuccessEvent(String refundId, String orderId, BigDecimal amount) {
        super(refundId); this.orderId = orderId; this.amount = amount;
    }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
}
