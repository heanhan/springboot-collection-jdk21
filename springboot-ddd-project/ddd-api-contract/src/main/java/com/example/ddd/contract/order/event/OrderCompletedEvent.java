package com.example.ddd.contract.order.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：订单已完成 (OrderCompleted)。
 *
 * <p><b>发布者：</b>order-service（在收到 logistics 的 ShipmentDeliveredEvent 或用户主动确认收货后触发）。</p>
 * <p><b>订阅者：</b>用户积分、评价邀请等扩展场景。</p>
 *
 * @author ddd-learning
 */
public class OrderCompletedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private String userId;
    private LocalDateTime completedAt;

    public OrderCompletedEvent() {
        super();
    }

    public OrderCompletedEvent(String orderId, String orderNo, String userId, LocalDateTime completedAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.completedAt = completedAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
