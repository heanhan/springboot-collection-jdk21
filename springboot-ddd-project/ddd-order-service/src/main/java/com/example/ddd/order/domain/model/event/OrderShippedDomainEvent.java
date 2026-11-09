package com.example.ddd.order.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：订单已发货（PAID → SHIPPED）。
 *
 * <p>领域内部语言，对外发布前由基础设施层翻译为集成事件。</p>
 */
public class OrderShippedDomainEvent extends AbstractDomainEvent {

    private final String orderNo;
    private final String userId;
    private final String shipmentId;
    private final String carrier;
    private final String trackingNo;
    private final LocalDateTime shippedAt;

    public OrderShippedDomainEvent(String orderId, String orderNo, String userId,
                                   String shipmentId, String carrier, String trackingNo, LocalDateTime shippedAt) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.shipmentId = shipmentId;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.shippedAt = shippedAt;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public String getShipmentId() { return shipmentId; }
    public String getCarrier() { return carrier; }
    public String getTrackingNo() { return trackingNo; }
    public LocalDateTime getShippedAt() { return shippedAt; }
}
