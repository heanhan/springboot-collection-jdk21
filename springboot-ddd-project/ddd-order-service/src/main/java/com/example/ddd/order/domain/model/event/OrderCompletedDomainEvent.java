package com.example.ddd.order.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：确认收货（SHIPPED → COMPLETED）。
 *
 * <p>领域内部语言，对外发布前由基础设施层翻译为集成事件。</p>
 */
public class OrderCompletedDomainEvent extends AbstractDomainEvent {

    private final String orderNo;
    private final String userId;
    private final LocalDateTime completedAt;

    public OrderCompletedDomainEvent(String orderId, String orderNo, String userId, LocalDateTime completedAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.completedAt = completedAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
