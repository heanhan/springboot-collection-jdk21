package com.example.ddd.order.infrastructure.rpc;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.inventory.InventoryFeignClient;
import com.example.ddd.contract.inventory.StockLockRequest;
import com.example.ddd.contract.inventory.StockOperationResultDTO;
import com.example.ddd.contract.inventory.StockReleaseRequest;
import com.example.ddd.order.application.port.InventoryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 适配器 (Adapter)：{@link InventoryGateway} 的 Feign 实现，即库存上下文的防腐层落地。
 *
 * <p><b>预占失败语义：</b>库存不足或服务不可用都抛 {@link BusinessException}，
 * 由订单应用服务的事务回滚消化（订单不落库）。</p>
 *
 * <p><b>释放尽力而为：</b>release 用于取消 / 超时补偿，即使远程失败也只记日志，
 * 不阻断本地取消事务；库存侧另有对账兜底。</p>
 */
@Component
public class InventoryGatewayImpl implements InventoryGateway {

    private static final Logger log = LoggerFactory.getLogger(InventoryGatewayImpl.class);

    private final InventoryFeignClient inventoryFeignClient;

    public InventoryGatewayImpl(InventoryFeignClient inventoryFeignClient) {
        this.inventoryFeignClient = inventoryFeignClient;
    }

    @Override
    public void lock(String bizNo, List<LockItem> items) {
        List<StockLockRequest.StockItem> stockItems = items.stream()
                .map(i -> new StockLockRequest.StockItem(i.skuId(), null, i.quantity()))
                .toList();
        Result<StockOperationResultDTO> result;
        try {
            result = inventoryFeignClient.lock(new StockLockRequest(bizNo, stockItems));
        } catch (Exception e) {
            log.error("[ACL] 调用库存服务预占失败 bizNo={}", bizNo, e);
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED, "库存服务暂不可用，请稍后重试", e);
        }
        if (result == null || !result.isSuccess() || result.getData() == null || !result.getData().success()) {
            String reason = result == null ? "无响应" : result.getMessage();
            throw new BusinessException(ErrorCode.INVENTORY_STOCK_SHORTAGE, "库存预占失败: " + reason);
        }
        log.info("[ACL] 库存预占成功 bizNo={} txId={}", bizNo, result.getData().transactionId());
    }

    @Override
    public void release(String bizNo, String reason) {
        try {
            Result<StockOperationResultDTO> result = inventoryFeignClient.release(new StockReleaseRequest(bizNo, reason));
            if (result == null || !result.isSuccess()) {
                log.warn("[ACL] 库存释放返回失败 bizNo={} result={}", bizNo, result);
            }
        } catch (Exception e) {
            // 释放失败不阻断取消流程，靠库存侧对账兜底
            log.error("[ACL] 调用库存服务释放失败 bizNo={}", bizNo, e);
        }
    }
}
