package com.example.ddd.inventory.application.command;

/**
 * 命令：人工调整库存 (Adjust)。
 *
 * <p>用于盘点、损耗、入库等运营场景，直接增减 available_qty。</p>
 *
 * @param warehouseId 仓库 ID
 * @param skuId       SKU ID
 * @param delta       变化量（正数入库，负数出库）
 * @param reason      调整原因，写入流水
 */
public record AdjustStockCommand(String warehouseId, String skuId, int delta, String reason) {
}
