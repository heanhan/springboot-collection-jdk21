package com.example.ddd.inventory.interfaces.rest;

import com.example.ddd.common.result.Result;
import com.example.ddd.inventory.application.command.AdjustStockCommand;
import com.example.ddd.inventory.application.service.StockApplicationService;
import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.interfaces.assembler.InventoryAssembler;
import com.example.ddd.inventory.interfaces.dto.InventoryRequests.AdjustStockRequest;
import com.example.ddd.inventory.interfaces.dto.InventoryResponses.StockView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：库存运营接口（面向后台管理，非服务间 Feign）。
 *
 * <p>提供库存查询、按 SKU 聚合查询、人工调整等能力。
 * 预占/实扣/释放属于服务间协作，放在 {@code InventoryInternalFacade}。</p>
 */
@RestController
@RequestMapping("/inventory/stocks")
public class StockController {

    private final StockApplicationService stockService;

    public StockController(StockApplicationService stockService) {
        this.stockService = stockService;
    }

    /**
     * 查询某仓库某 SKU 的库存。
     */
    @GetMapping("/{warehouseId}/{skuId}")
    public Result<StockView> getStock(@PathVariable("warehouseId") String warehouseId,
                                      @PathVariable("skuId") String skuId) {
        return stockService.getStock(warehouseId, skuId)
                .map(s -> Result.ok(InventoryAssembler.toView(s)))
                .orElseGet(() -> Result.fail("4002", "库存记录不存在"));
    }

    /**
     * 查询某 SKU 在所有仓库的库存分布。
     */
    @GetMapping("/sku/{skuId}")
    public Result<List<StockView>> listBySku(@PathVariable("skuId") String skuId) {
        List<StockView> views = stockService.listBySku(skuId).stream()
                .map(InventoryAssembler::toView).toList();
        return Result.ok(views);
    }

    /**
     * 人工调整库存（盘点 / 损耗 / 入库）。
     */
    @PostMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody AdjustStockRequest request) {
        stockService.adjust(new AdjustStockCommand(request.warehouseId(), request.skuId(),
                request.delta(), request.reason()));
        return Result.ok();
    }
}
