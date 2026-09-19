package com.example.ddd.inventory.interfaces.facade;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.inventory.StockDTO;
import com.example.ddd.contract.inventory.StockDeductRequest;
import com.example.ddd.contract.inventory.StockLockRequest;
import com.example.ddd.contract.inventory.StockOperationResultDTO;
import com.example.ddd.contract.inventory.StockReleaseRequest;
import com.example.ddd.inventory.application.command.LockStockCommand;
import com.example.ddd.inventory.application.command.StockOperationResult;
import com.example.ddd.inventory.application.service.StockApplicationService;
import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.interfaces.assembler.InventoryAssembler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Facade：实现 {@code com.example.ddd.contract.inventory.InventoryFeignClient} 的服务端点。
 *
 * <p><b>路径必须与契约一致：</b>{@code /inventory/internal/**}。
 * 其他服务（order / logistics）通过 Feign 调用这些端点完成预占、实扣、释放。</p>
 *
 * <p><b>本层职责：</b>只做 契约请求 DTO ⇄ 应用层 Command 的转换 + 调用应用服务 + 装配响应，
 * 不含任何库存业务规则。</p>
 */
@RestController
@RequestMapping("/inventory/internal")
public class InventoryInternalFacade {

    private final StockApplicationService stockService;

    public InventoryInternalFacade(StockApplicationService stockService) {
        this.stockService = stockService;
    }

    /**
     * 预占库存（下单调用，bizNo 幂等）。
     */
    @PostMapping("/stock/lock")
    public Result<StockOperationResultDTO> lock(@RequestBody StockLockRequest request) {
        List<LockStockCommand.Item> items = request.items() == null ? List.of()
                : request.items().stream()
                .map(i -> new LockStockCommand.Item(i.skuId(), i.warehouseId(), i.quantity()))
                .toList();
        StockOperationResult result = stockService.lock(new LockStockCommand(request.bizNo(), items));
        return Result.ok(InventoryAssembler.toContractDTO(result));
    }

    /**
     * 实扣库存（支付成功后调用）。
     */
    @PostMapping("/stock/deduct")
    public Result<StockOperationResultDTO> deduct(@RequestBody StockDeductRequest request) {
        StockOperationResult result = stockService.deduct(request.bizNo());
        return Result.ok(InventoryAssembler.toContractDTO(result));
    }

    /**
     * 释放预占（订单取消 / 超时调用）。
     */
    @PostMapping("/stock/release")
    public Result<StockOperationResultDTO> release(@RequestBody StockReleaseRequest request) {
        StockOperationResult result = stockService.release(request.bizNo(), request.reason());
        return Result.ok(InventoryAssembler.toContractDTO(result));
    }

    /**
     * 查询某 SKU 在某仓库的库存。
     */
    @GetMapping("/stock/{warehouseId}/{skuId}")
    public Result<StockDTO> getStock(@PathVariable("warehouseId") String warehouseId,
                                     @PathVariable("skuId") String skuId) {
        Optional<Stock> stock = stockService.getStock(warehouseId, skuId);
        if (stock.isEmpty()) {
            return Result.fail("4002", "库存记录不存在: warehouseId=" + warehouseId + " skuId=" + skuId);
        }
        return Result.ok(InventoryAssembler.toContractDTO(stock.get()));
    }
}
