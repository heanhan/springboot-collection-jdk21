package com.example.ddd.order.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：订单已取消（→ CANCELLED）。
 *
 * <p>领域内部语言，对外发布前由基础设施层翻译为集成事件。</p>
 */
public class OrderCancelledDomainEvent extends AbstractDomainEvent {

    private final String orderNo;
    private final String userId;
    private final String reasonCode;
    private final String reasonDesc;

    public OrderCancelledDomainEvent(String orderId, String orderNo, String userId,
                                     String reasonCode, String reasonDesc) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonDesc() { return reasonDesc; }
}
