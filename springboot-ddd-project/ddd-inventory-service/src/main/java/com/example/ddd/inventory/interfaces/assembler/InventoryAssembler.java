package com.example.ddd.inventory.interfaces.assembler;

import com.example.ddd.contract.inventory.StockDTO;
import com.example.ddd.contract.inventory.StockOperationResultDTO;
import com.example.ddd.inventory.application.command.StockOperationResult;
import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.domain.model.aggregate.Warehouse;
import com.example.ddd.inventory.interfaces.dto.InventoryResponses.StockView;
import com.example.ddd.inventory.interfaces.dto.InventoryResponses.WarehouseView;

import java.util.List;

/**
 * 装配器 (Assembler)：领域对象 / 应用层结果 ⇄ DTO。
 *
 * <p><b>为什么单独一层装配器？</b>
 * 隔离"对外展示模型"与"领域模型"。领域模型演进（加字段、改类型）不应直接冲击 API 契约，
 * 装配器是二者之间的防腐翻译层。</p>
 */
public final class InventoryAssembler {

    private InventoryAssembler() {}

    /** 应用层结果 -> 契约 DTO（供 Feign 调用方使用）。 */
    public static StockOperationResultDTO toContractDTO(StockOperationResult result) {
        List<StockOperationResultDTO.Failure> failures = result.failures() == null ? List.of()
                : result.failures().stream()
                .map(f -> new StockOperationResultDTO.Failure(f.skuId(), f.warehouseId(),
                        f.requested(), f.available(), f.reason()))
                .toList();
        return new StockOperationResultDTO(result.bizNo(), result.success(),
                result.transactionId(), failures);
    }

    /** 领域 Stock -> 契约 StockDTO。 */
    public static StockDTO toContractDTO(Stock stock) {
        return new StockDTO(stock.getWarehouseId(), stock.getSkuId(),
                stock.getAvailableQty(), stock.getLockedQty(), stock.getTotalQty());
    }

    /** 领域 Stock -> 运营视图。 */
    public static StockView toView(Stock stock) {
        return new StockView(stock.getStockId(), stock.getWarehouseId(), stock.getSkuId(),
                stock.getAvailableQty(), stock.getLockedQty(), stock.getTotalQty(),
                stock.getWarnQty(), stock.isBelowWarning());
    }

    /** 领域 Warehouse -> 运营视图。 */
    public static WarehouseView toView(Warehouse w) {
        return new WarehouseView(w.getWarehouseId(), w.getCode(), w.getName(),
                w.getProvince(), w.getCity(), w.getDistrict(), w.getAddress(),
                w.getContact(), w.getPhone(), w.getPriority(), w.isEnabled());
    }
}
