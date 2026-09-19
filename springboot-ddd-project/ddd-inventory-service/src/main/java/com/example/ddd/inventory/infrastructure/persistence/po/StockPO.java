package com.example.ddd.inventory.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：t_stock 表映射（Stock 聚合根）。
 *
 * <p><b>乐观锁：</b>{@code version} 字段继承自 {@link AbstractJpaAuditablePO} 上的 {@code @Version}。
 * 高并发预占时，JPA 会在 UPDATE 加 {@code WHERE version = ?}，冲突抛
 * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}，由应用层重试。</p>
 */
@Entity
@Table(name = "t_stock")
public class StockPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "stock_id", length = 32, nullable = false)
    private String stockId;

    @Column(name = "warehouse_id", length = 32, nullable = false)
    private String warehouseId;

    @Column(name = "sku_id", length = 32, nullable = false)
    private String skuId;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty;

    @Column(name = "locked_qty", nullable = false)
    private Integer lockedQty;

    @Column(name = "warn_qty", nullable = false)
    private Integer warnQty;

    public String getStockId() { return stockId; }
    public void setStockId(String stockId) { this.stockId = stockId; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public Integer getAvailableQty() { return availableQty; }
    public void setAvailableQty(Integer availableQty) { this.availableQty = availableQty; }
    public Integer getLockedQty() { return lockedQty; }
    public void setLockedQty(Integer lockedQty) { this.lockedQty = lockedQty; }
    public Integer getWarnQty() { return warnQty; }
    public void setWarnQty(Integer warnQty) { this.warnQty = warnQty; }
}
