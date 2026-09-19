package com.example.ddd.inventory.domain.model.entity;

import com.example.ddd.inventory.domain.model.valueobject.TransactionType;

import java.time.LocalDateTime;

/**
 * 实体：StockTransaction 库存流水（Append-only Log）。
 *
 * <p><b>为什么是"追加式"而不是可变实体？</b>
 * 库存流水是<b>历史事实</b>，一旦写入不能修改；这符合事件溯源 (Event Sourcing) 的思想。
 * 任何一次库存变化都必须留下痕迹，便于：
 * <ul>
 *   <li>幂等：同一 (bizNo, type, skuId, warehouseId) 只处理一次。</li>
 *   <li>审计：谁在什么时间做了什么操作。</li>
 *   <li>对账：与订单/支付流水交叉核对。</li>
 * </ul>
 *
 * <p><b>为什么不是聚合根？</b>
 * StockTransaction 没有独立生命周期，它依附于 Stock 变化产生。
 * 但为了持久化方便，本项目把它作为独立实体（有自己的 Repository）。</p>
 */
public class StockTransaction {

    private final String transactionId;
    private final String bizNo;
    private final TransactionType type;
    private final String warehouseId;
    private final String skuId;
    private final int quantity;
    private final int beforeQty;
    private final int afterQty;
    private final int beforeLocked;
    private final int afterLocked;
    private final String reason;
    private final LocalDateTime createTime;

    public StockTransaction(String transactionId, String bizNo, TransactionType type,
                            String warehouseId, String skuId, int quantity,
                            int beforeQty, int afterQty, int beforeLocked, int afterLocked,
                            String reason, LocalDateTime createTime) {
        this.transactionId = transactionId;
        this.bizNo = bizNo;
        this.type = type;
        this.warehouseId = warehouseId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.beforeQty = beforeQty;
        this.afterQty = afterQty;
        this.beforeLocked = beforeLocked;
        this.afterLocked = afterLocked;
        this.reason = reason;
        this.createTime = createTime == null ? LocalDateTime.now() : createTime;
    }

    public String getTransactionId() { return transactionId; }
    public String getBizNo() { return bizNo; }
    public TransactionType getType() { return type; }
    public String getWarehouseId() { return warehouseId; }
    public String getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public int getBeforeQty() { return beforeQty; }
    public int getAfterQty() { return afterQty; }
    public int getBeforeLocked() { return beforeLocked; }
    public int getAfterLocked() { return afterLocked; }
    public String getReason() { return reason; }
    public LocalDateTime getCreateTime() { return createTime; }
}
