package com.example.ddd.payment.infrastructure.persistence.po;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA 退款持久化对象：payment_id 唯一约束限制为一次全额退款。
 */
@Entity
@Table(name = "t_refund", uniqueConstraints = @UniqueConstraint(columnNames = "payment_id"))
public class RefundPO {
    @Id
    @Column(length = 64)
    public String refundId;
    @Column(name = "payment_id", length = 64, nullable = false)
    public String paymentId;
    @Column(length = 64)
    public String orderId;
    @Column(precision = 19, scale = 2)
    public BigDecimal amount;
    @Column(length = 255)
    public String reason;
    @Column(length = 16)
    public String status;
    public LocalDateTime createdAt;
}
