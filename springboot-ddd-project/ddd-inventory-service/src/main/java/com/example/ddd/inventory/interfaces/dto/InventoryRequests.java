package com.example.ddd.inventory.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 库存/仓库接口的请求 DTO 集合（用嵌套 record 归类，减少文件散落）。
 */
public final class InventoryRequests {

    private InventoryRequests() {}

    /**
     * 人工调整库存请求。
     *
     * @param warehouseId 仓库 ID
     * @param skuId       SKU ID
     * @param delta       变化量（正数入库，负数出库）
     * @param reason      调整原因
     */
    public record AdjustStockRequest(@NotBlank String warehouseId,
                                     @NotBlank String skuId,
                                     int delta,
                                     String reason) {
    }

    /**
     * 创建仓库请求。
     */
    public record CreateWarehouseRequest(@NotBlank String code,
                                         @NotBlank String name,
                                         String province,
                                         String city,
                                         String district,
                                         String address,
                                         String contact,
                                         String phone,
                                         @Min(0) int priority) {
    }

    /**
     * 更新仓库请求（字段为 null 表示不修改）。
     */
    public record UpdateWarehouseRequest(String name,
                                         String province,
                                         String city,
                                         String district,
                                         String address,
                                         String contact,
                                         String phone,
                                         Integer priority,
                                         Boolean enabled) {
    }
}
