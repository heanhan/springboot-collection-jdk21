package com.example.ddd.payment.infrastructure.persistence.po;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA 支付持久化对象：无业务行为，独立于领域模型；订单唯一约束防止重复建单。
 */
@Entity
@Table(name = "t_payment", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class PaymentPO {
    @Id
    @Column(length = 64)
    public String paymentId;
    @Column(name = "order_id", length = 64, nullable = false)
    public String orderId;
    @Column(length = 64)
    public String orderNo;
    @Column(length = 64)
    public String userId;
    @Column(precision = 19, scale = 2)
    public BigDecimal amount;
    @Column(length = 16)
    public String channel;
    @Column(length = 16)
    public String status;
    @Column(length = 128)
    public String tradeNo;
    public LocalDateTime createdAt;
    public LocalDateTime paidAt;
    public LocalDateTime expireAt;
    @Version
    public Long version;
}
