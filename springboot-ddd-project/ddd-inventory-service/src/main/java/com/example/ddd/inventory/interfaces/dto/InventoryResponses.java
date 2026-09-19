package com.example.ddd.inventory.interfaces.dto;

/**
 * 库存/仓库接口的响应 DTO 集合。
 *
 * <p><b>为什么与契约 DTO 分开？</b>
 * 契约 DTO（{@code StockDTO}）用于服务间 Feign 调用，字段最小化；
 * 本处 View 面向运营后台，可包含更多展示字段（如预警阈值、仓库地址）。</p>
 */
public final class InventoryResponses {

    private InventoryResponses() {}

    /**
     * 库存视图。
     *
     * @param stockId      库存记录 ID
     * @param warehouseId  仓库 ID
     * @param skuId        SKU ID
     * @param availableQty 可用库存
     * @param lockedQty    预占库存
     * @param totalQty     总库存
     * @param warnQty      预警阈值
     * @param belowWarning 是否低于预警
     */
    public record StockView(String stockId, String warehouseId, String skuId,
                            int availableQty, int lockedQty, int totalQty,
                            int warnQty, boolean belowWarning) {
    }

    /**
     * 仓库视图。
     */
    public record WarehouseView(String warehouseId, String code, String name,
                                String province, String city, String district, String address,
                                String contact, String phone, int priority, boolean enabled) {
    }
}
