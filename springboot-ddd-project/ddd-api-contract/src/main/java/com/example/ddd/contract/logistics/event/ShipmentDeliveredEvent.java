package com.example.ddd.contract.logistics.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：包裹已签收 (ShipmentDelivered)。
 *
 * <p><b>发布者：</b>logistics-service（承运商回调或用户主动确认收货）。</p>
 * <p><b>订阅者：</b>order-service（把订单状态改为 COMPLETED）。</p>
 *
 * @author ddd-learning
 */
public class ShipmentDeliveredEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderNo;
    private String userId;
    private LocalDateTime deliveredAt;
    /** 签收人 */
    private String signedBy;

    public ShipmentDeliveredEvent() {
        super();
    }

    public ShipmentDeliveredEvent(String shipmentId, String orderId, String orderNo, String userId,
                                  LocalDateTime deliveredAt, String signedBy) {
        super(shipmentId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.deliveredAt = deliveredAt;
        this.signedBy = signedBy;
    }

    public String getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public String getSignedBy() { return signedBy; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }
}
