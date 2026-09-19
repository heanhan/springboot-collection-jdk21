package com.example.ddd.contract.inventory;

import java.io.Serializable;
import java.util.List;

/**
 * 库存预占请求。
 *
 * @param bizNo    业务号（幂等键），一般传 orderId
 * @param items    预占明细
 */
public record StockLockRequest(String bizNo, List<StockItem> items) implements Serializable {

    /**
     * 单个 SKU 的预占明细。
     *
     * @param skuId       SKU ID
     * @param warehouseId 仓库 ID（可选，为 null 时由库存服务根据策略自动分配）
     * @param quantity    数量，必须 > 0
     */
    public record StockItem(String skuId, String warehouseId, int quantity) implements Serializable {
    }
}
