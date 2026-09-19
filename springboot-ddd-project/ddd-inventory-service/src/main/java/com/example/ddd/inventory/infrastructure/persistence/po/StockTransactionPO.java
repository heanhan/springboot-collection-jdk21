package com.example.ddd.inventory.infrastructure.persistence.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.example.ddd.inventory.domain.model.valueobject.TransactionType;

import java.time.LocalDateTime;

/**
 * PO：t_stock_transaction 表映射（追加式流水）。
 *
 * <p><b>为什么不继承 {@code AbstractJpaAuditablePO}？</b>
 * 流水是"历史事实"，只写不改，因此没有 {@code update_time / version / deleted} 字段，
 * 只有 {@code create_time}。乐观锁、软删除对它没有意义。</p>
 */
@Entity
@Table(name = "t_stock_transaction")
public class StockTransactionPO {

    @Id
    @Column(name = "transaction_id", length = 32, nullable = false)
    private String transactionId;

    @Column(name = "biz_no", length = 64, nullable = false)
    private String bizNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    private TransactionType type;

    @Column(name = "warehouse_id", length = 32, nullable = false)
    private String warehouseId;

    @Column(name = "sku_id", length = 32, nullable = false)
    private String skuId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    @Column(name = "before_locked", nullable = false)
    private Integer beforeLocked;

    @Column(name = "after_locked", nullable = false)
    private Integer afterLocked;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getBeforeQty() { return beforeQty; }
    public void setBeforeQty(Integer beforeQty) { this.beforeQty = beforeQty; }
    public Integer getAfterQty() { return afterQty; }
    public void setAfterQty(Integer afterQty) { this.afterQty = afterQty; }
    public Integer getBeforeLocked() { return beforeLocked; }
    public void setBeforeLocked(Integer beforeLocked) { this.beforeLocked = beforeLocked; }
    public Integer getAfterLocked() { return afterLocked; }
    public void setAfterLocked(Integer afterLocked) { this.afterLocked = afterLocked; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
