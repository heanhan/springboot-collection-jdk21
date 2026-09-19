package com.example.ddd.order.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PO：t_order 表映射（Order 聚合根）。
 *
 * <p><b>金额存储：</b>领域层用 {@code Money}（含币种），PO 只存 {@code BigDecimal}（币种恒为 CNY，
 * 由 Converter 重建）。这样表结构不感知值对象细节。</p>
 *
 * <p><b>地址快照：</b>收货地址拆成多列存储（province/city/district/detail/zipcode/receiver/mobile），
 * 由 Converter 组装回 {@code Address} 值对象。</p>
 */
@Entity
@Table(name = "t_order")
public class OrderPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "order_id", length = 32, nullable = false)
    private String orderId;

    @Column(name = "order_no", length = 32, nullable = false)
    private String orderNo;

    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal shippingFee;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "pay_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal payAmount;

    @Column(name = "receiver", length = 64, nullable = false)
    private String receiver;

    @Column(name = "receiver_mobile", length = 20, nullable = false)
    private String receiverMobile;

    @Column(name = "shipping_province", length = 32, nullable = false)
    private String shippingProvince;

    @Column(name = "shipping_city", length = 32, nullable = false)
    private String shippingCity;

    @Column(name = "shipping_district", length = 32, nullable = false)
    private String shippingDistrict;

    @Column(name = "shipping_detail", length = 255, nullable = false)
    private String shippingDetail;

    @Column(name = "shipping_zipcode", length = 16)
    private String shippingZipcode;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "payment_channel", length = 16)
    private String paymentChannel;

    @Column(name = "payment_trade_no", length = 64)
    private String paymentTradeNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getReceiverMobile() { return receiverMobile; }
    public void setReceiverMobile(String receiverMobile) { this.receiverMobile = receiverMobile; }
    public String getShippingProvince() { return shippingProvince; }
    public void setShippingProvince(String shippingProvince) { this.shippingProvince = shippingProvince; }
    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
    public String getShippingDistrict() { return shippingDistrict; }
    public void setShippingDistrict(String shippingDistrict) { this.shippingDistrict = shippingDistrict; }
    public String getShippingDetail() { return shippingDetail; }
    public void setShippingDetail(String shippingDetail) { this.shippingDetail = shippingDetail; }
    public String getShippingZipcode() { return shippingZipcode; }
    public void setShippingZipcode(String shippingZipcode) { this.shippingZipcode = shippingZipcode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public String getPaymentTradeNo() { return paymentTradeNo; }
    public void setPaymentTradeNo(String paymentTradeNo) { this.paymentTradeNo = paymentTradeNo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
