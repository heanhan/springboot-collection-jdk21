package com.example.ddd.logistics.infrastructure.persistence.po;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** JPA 发货单：查询字段结构化，有限大小的商品/轨迹列表作为聚合内部 JSON 快照保存。 */
@Entity
@Table(name="t_shipment",uniqueConstraints=@UniqueConstraint(columnNames="order_id"))
public class ShipmentPO {
    @Id @Column(length=64) public String shipmentId;
    @Column(name="order_id",length=64,nullable=false) public String orderId;
    @Column(length=64) public String orderNo;
    @Column(length=64) public String userId;
    @Column(length=16) public String carrier;
    @Column(length=128) public String trackingNo;
    @Column(length=16) public String status;
    @Column(length=64) public String receiver;
    @Column(length=32) public String mobile;
    @Column(length=512) public String address;
    public LocalDateTime shippedAt;
    public LocalDateTime deliveredAt;
    @Column(columnDefinition="LONGTEXT") public String items;
    @Column(columnDefinition="LONGTEXT") public String tracks;
    @Version public Long version;
}
