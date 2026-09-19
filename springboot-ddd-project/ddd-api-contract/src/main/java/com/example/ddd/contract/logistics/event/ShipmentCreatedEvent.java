package com.example.ddd.contract.logistics.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：发货单已创建 (ShipmentCreated)。
 *
 * <p><b>发布者：</b>logistics-service（在收到 OrderPaidEvent 后创建发货单）。</p>
 * <p><b>订阅者：</b>order-service（把订单状态改为 SHIPPED，再发 OrderShippedEvent）。</p>
 *
 * @author ddd-learning
 */
public class ShipmentCreatedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderNo;
    private String userId;
    private String carrier;
    private String trackingNo;
    private LocalDateTime shippedAt;

    public ShipmentCreatedEvent() {
        super();
    }

    public ShipmentCreatedEvent(String shipmentId, String orderId, String orderNo, String userId,
                                String carrier, String trackingNo, LocalDateTime shippedAt) {
        super(shipmentId);
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.shippedAt = shippedAt;
    }

    public String getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public String getCarrier() { return carrier; }
    public String getTrackingNo() { return trackingNo; }
    public LocalDateTime getShippedAt() { return shippedAt; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
}
