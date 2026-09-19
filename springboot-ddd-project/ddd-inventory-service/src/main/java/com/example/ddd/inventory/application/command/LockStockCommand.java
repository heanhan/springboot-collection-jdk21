package com.example.ddd.inventory.application.command;

import java.util.List;

/**
 * 命令：预占库存 (Lock)。
 *
 * <p><b>为什么用 record？</b>命令是不可变的用例输入，record 语义最贴切。</p>
 *
 * @param bizNo 业务号（幂等键），通常是 orderId
 * @param items 预占明细
 */
public record LockStockCommand(String bizNo, List<Item> items) {

    /**
     * 单条预占明细。
     *
     * @param skuId       SKU ID
     * @param warehouseId 指定仓库（可为 null，由分配策略自动选仓）
     * @param quantity    数量（&gt; 0）
     */
    public record Item(String skuId, String warehouseId, int quantity) {
    }
}
