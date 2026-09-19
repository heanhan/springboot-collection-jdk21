package com.example.ddd.contract.inventory;

import java.io.Serializable;

/**
 * 库存查询 DTO。
 *
 * @param warehouseId   仓库 ID
 * @param skuId         SKU ID
 * @param availableQty  可用库存
 * @param lockedQty     预占库存
 * @param totalQty      总库存 = available + locked
 */
public record StockDTO(String warehouseId,
                       String skuId,
                       int availableQty,
                       int lockedQty,
                       int totalQty) implements Serializable {
}
