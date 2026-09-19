package com.example.ddd.contract.order.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

import java.time.LocalDateTime;

/**
 * 领域事件：订单已发货 (OrderShipped)。
 *
 * <p><b>发布者：</b>order-service（在收到 logistics-service 的 ShipmentCreatedEvent 后触发）。</p>
 * <p><b>订阅者：</b>notification-service（本项目未实现，可作扩展点）。</p>
 *
 * @author ddd-learning
 */
public class OrderShippedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private String userId;
    /** 发货单号 */
    private String shipmentId;
    /** 承运商 */
    private String carrier;
    /** 运单号 */
    private String trackingNo;
    /** 发货时间 */
    private LocalDateTime shippedAt;

    public OrderShippedEvent() {
        super();
    }

    public OrderShippedEvent(String orderId, String orderNo, String userId,
                             String shipmentId, String carrier, String trackingNo,
                             LocalDateTime shippedAt) {
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

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
}
