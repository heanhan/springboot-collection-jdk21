package com.example.ddd.inventory.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.inventory.domain.model.valueobject.TransactionType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 聚合根：Stock 库存。
 *
 * <p><b>为什么单独一个聚合而不是放在 Warehouse 聚合内？</b>
 * <ul>
 *   <li>并发扣减：把 Stock 放到 Warehouse 内，任何一次扣减都要锁整个仓库聚合，冲突率极高。</li>
 *   <li>数据规模：一个仓库可能有几万条 SKU，聚合过大会导致加载/持久化都很慢。</li>
 *   <li>不变式：Stock 的不变式（available &gt;= 0）只涉及自己，不需要跨对象协调。</li>
 * </ul>
 *
 * <p><b>不变式：</b>
 * <ol>
 *   <li>availableQty &gt;= 0。</li>
 *   <li>lockedQty &gt;= 0。</li>
 *   <li>扣减时 availableQty 必须 &gt;= qty，否则抛"库存不足"。</li>
 * </ol>
 *
 * <p><b>并发控制：</b>PO 层使用 {@code @Version} 乐观锁。
 * 应用层配置重试（默认 3 次）：冲突时抛 {@link org.springframework.orm.ObjectOptimisticLockingFailureException}，
 * 由重试逻辑捕获后重新加载 + 重试。</p>
 */
public class Stock extends BaseAggregateRoot {

    private final String stockId;
    private final String warehouseId;
    private final String skuId;
    private int availableQty;
    private int lockedQty;
    private int warnQty;
    private long version;

    public static Stock create(String stockId, String warehouseId, String skuId,
                               int initialQty, int warnQty) {
        if (initialQty < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "初始库存不能为负");
        }
        Stock s = new Stock(stockId, warehouseId, skuId);
        s.availableQty = initialQty;
        s.lockedQty = 0;
        s.warnQty = warnQty;
        return s;
    }

    public static Stock reconstitute(String stockId, String warehouseId, String skuId,
                                     int availableQty, int lockedQty, int warnQty, long version) {
        Stock s = new Stock(stockId, warehouseId, skuId);
        s.availableQty = availableQty;
        s.lockedQty = lockedQty;
        s.warnQty = warnQty;
        s.version = version;
        return s;
    }

    private Stock(String stockId, String warehouseId, String skuId) {
        this.stockId = Objects.requireNonNull(stockId);
        this.warehouseId = Objects.requireNonNull(warehouseId);
        this.skuId = Objects.requireNonNull(skuId);
    }

    // ============================================================
    // 业务行为：库存四态操作
    // ============================================================

    /**
     * 预占库存（下单）。
     *
     * @param qty 预占数量，必须 &gt; 0
     * @throws BusinessException 库存不足时抛 {@link ErrorCode#INVENTORY_STOCK_SHORTAGE}
     */
    public void lock(int qty) {
        assertPositive(qty);
        if (this.availableQty < qty) {
            throw new BusinessException(ErrorCode.INVENTORY_STOCK_SHORTAGE,
                    String.format("库存不足: skuId=%s 需要=%d 可用=%d", skuId, qty, availableQty));
        }
        this.availableQty -= qty;
        this.lockedQty += qty;
    }

    /**
     * 释放预占（订单取消 / 超时）。
     */
    public void unlock(int qty) {
        assertPositive(qty);
        if (this.lockedQty < qty) {
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED,
                    String.format("预占库存不足: skuId=%s 请求释放=%d 已预占=%d", skuId, qty, lockedQty));
        }
        this.lockedQty -= qty;
        this.availableQty += qty;
    }

    /**
     * 实扣（支付成功后）。locked -= qty，availableQty 不变（下单时已扣）。
     */
    public void deduct(int qty) {
        assertPositive(qty);
        if (this.lockedQty < qty) {
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED,
                    String.format("预占库存不足以实扣: skuId=%s 请求=%d 已预占=%d", skuId, qty, lockedQty));
        }
        this.lockedQty -= qty;
    }

    /**
     * 撤销实扣（退款）。available += qty。
     */
    public void rollbackDeduct(int qty) {
        assertPositive(qty);
        this.availableQty += qty;
    }

    /**
     * 人工调整（盘点/损耗）：直接改 availableQty。
     */
    public void adjust(int delta, String reason) {
        int newQty = this.availableQty + delta;
        if (newQty < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "调整后库存不能为负: 当前=" + availableQty + " delta=" + delta);
        }
        this.availableQty = newQty;
    }

    public boolean isBelowWarning() {
        return this.availableQty <= this.warnQty;
    }

    private void assertPositive(int qty) {
        if (qty <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "操作数量必须大于 0");
        }
    }

    // ============================================================
    // Getters
    // ============================================================

    @Override
    public String aggregateId() {
        return stockId;
    }

    public String getStockId() { return stockId; }
    public String getWarehouseId() { return warehouseId; }
    public String getSkuId() { return skuId; }
    public int getAvailableQty() { return availableQty; }
    public int getLockedQty() { return lockedQty; }
    public int getWarnQty() { return warnQty; }
    public long getVersion() { return version; }
    public int getTotalQty() { return availableQty + lockedQty; }

    public void setVersion(long version) { this.version = version; }
    public void setWarnQty(int warnQty) { this.warnQty = warnQty; }
}
