package com.example.ddd.contract.inventory;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务对外 Feign 契约。
 *
 * <p><b>库存操作三阶段：</b>
 * <ol>
 *   <li><b>预占 (lock)</b>：下单时把 available_qty 减掉、locked_qty 加上去；
 *       此时商品还未真正售出，但也不能再被别人下单。</li>
 *   <li><b>实扣 (deduct)</b>：支付成功后，locked_qty 减掉，实际库存永久减少。</li>
 *   <li><b>回滚 (release)</b>：订单取消或超时未支付，locked_qty 归还给 available_qty。</li>
 * </ol>
 * 这是电商库存的经典模型，能应对"下单未支付"的中间态。</p>
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.INVENTORY, contextId = "inventoryFeignClient", path = "/inventory/internal",
        url = "${ddd.services.inventory-url:}")
public interface InventoryFeignClient {

    /**
     * 预占库存（下单调用）。
     * <p>使用 bizNo (通常是 orderId) 做幂等，重复调用同一 bizNo 返回同样结果。</p>
     */
    @PostMapping("/stock/lock")
    Result<StockOperationResultDTO> lock(@RequestBody StockLockRequest request);

    /**
     * 实扣库存（支付成功后由 logistics 或 order 服务调用）。
     */
    @PostMapping("/stock/deduct")
    Result<StockOperationResultDTO> deduct(@RequestBody StockDeductRequest request);

    /**
     * 释放预占（订单取消 / 超时）。
     */
    @PostMapping("/stock/release")
    Result<StockOperationResultDTO> release(@RequestBody StockReleaseRequest request);

    /**
     * 查询某 SKU 在某仓库的可用库存。
     */
    @GetMapping("/stock/{warehouseId}/{skuId}")
    Result<StockDTO> getStock(@PathVariable("warehouseId") String warehouseId,
                              @PathVariable("skuId") String skuId);
}
