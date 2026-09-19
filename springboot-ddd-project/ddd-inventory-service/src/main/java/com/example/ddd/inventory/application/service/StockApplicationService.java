package com.example.ddd.inventory.application.service;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.contract.inventory.event.StockDeductedEvent;
import com.example.ddd.contract.inventory.event.StockLockedEvent;
import com.example.ddd.contract.inventory.event.StockReleasedEvent;
import com.example.ddd.inventory.application.command.AdjustStockCommand;
import com.example.ddd.inventory.application.command.LockStockCommand;
import com.example.ddd.inventory.application.command.StockOperationResult;
import com.example.ddd.inventory.application.port.DomainEventPublisher;
import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.domain.model.entity.StockTransaction;
import com.example.ddd.inventory.domain.model.valueobject.TransactionType;
import com.example.ddd.inventory.domain.repository.StockRepository;
import com.example.ddd.inventory.domain.repository.StockTransactionRepository;
import com.example.ddd.inventory.domain.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 应用服务：库存操作用例（预占 / 实扣 / 释放 / 调整 / 查询）。
 *
 * <p><b>用例编排：</b>应用服务负责事务边界、幂等控制、乐观锁重试、跨聚合协调（调用领域服务）、
 * 事务提交后发布领域事件；<b>不包含</b>库存增减的业务规则（那属于 {@link Stock} 聚合根）。</p>
 *
 * <p><b>为什么用 {@link TransactionTemplate} 而不是 {@code @Transactional}？</b>
 * 乐观锁冲突需要在<b>事务外</b>重试：若用 {@code @Transactional} 注解，方法内 catch 到冲突时事务已被标记 rollback-only，
 * 无法在同一方法内重试。用编程式事务把"一次尝试"包成独立事务，冲突后重新开启新事务重试，语义清晰。</p>
 *
 * <p><b>幂等键：</b>{@code bizNo + type + warehouseId + skuId}，落库前查重；命令级再用 {@code existsByBizNoAndType} 快速短路。</p>
 */
@Service
public class StockApplicationService {

    private static final Logger log = LoggerFactory.getLogger(StockApplicationService.class);

    private final StockRepository stockRepository;
    private final StockTransactionRepository transactionRepository;
    private final StockReservationService reservationService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final int maxRetry;
    private final com.example.ddd.inventory.application.port.StockOperationGuard operationGuard;

    public StockApplicationService(StockRepository stockRepository,
                                   StockTransactionRepository transactionRepository,
                                   StockReservationService reservationService,
                                   DomainEventPublisher eventPublisher,
                                   TransactionTemplate transactionTemplate,
                                   @Value("${ddd.inventory.lock-max-retry:3}") int maxRetry,
                                   com.example.ddd.inventory.application.port.StockOperationGuard operationGuard) {
        this.operationGuard = operationGuard;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.maxRetry = Math.max(1, maxRetry);
    }

    // ============================================================
    // 用例 1：预占库存 (下单)
    // ============================================================

    /**
     * 预占库存。事务提交后发布 {@link StockLockedEvent}。
     *
     * @param cmd 预占命令
     * @return 操作结果
     */
    public StockOperationResult lock(LockStockCommand cmd) {
        validateBizNo(cmd.bizNo());
        if (cmd.items() == null || cmd.items().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预占明细不能为空");
        }
        // 命令级幂等：已预占过则直接返回成功（重放）
        if (cmd.items().stream().map(LockStockCommand.Item::skuId).distinct().count() != cmd.items().size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预占请求中 SKU 不得重复");
        }

        List<DomainEvent> pendingEvents = new ArrayList<>();
        String transactionId = executeWithRetry("lock", cmd.bizNo(), () -> {
            pendingEvents.clear();
            if (transactionRepository.existsByBizNoAndType(cmd.bizNo(), TransactionType.LOCK))
                return firstTransactionId(cmd.bizNo(), TransactionType.LOCK);
            return doLock(cmd, pendingEvents);
        });

        publishAfterCommit(pendingEvents);
        log.info("[Inventory] lock success bizNo={} txId={}", cmd.bizNo(), transactionId);
        return StockOperationResult.ok(cmd.bizNo(), transactionId, false);
    }

    /** 事务内的预占逻辑：分配 -> 扣可用/加预占 -> 写流水 -> 收集事件。返回首条流水 ID。 */
    private String doLock(LockStockCommand cmd, List<DomainEvent> pendingEvents) {
        String firstTxId = null;
        String primaryWarehouse = null;
        for (LockStockCommand.Item item : cmd.items()) {
            List<StockReservationService.Allocation> allocations =
                    reservationService.allocate(item.skuId(), item.quantity(), item.warehouseId());
            for (StockReservationService.Allocation alloc : allocations) {
                // 明细级幂等
                if (transactionRepository.findIdempotent(cmd.bizNo(), TransactionType.LOCK,
                        alloc.warehouseId(), alloc.skuId()).isPresent()) {
                    continue;
                }
                Stock stock = loadStock(alloc.warehouseId(), alloc.skuId());
                int beforeQty = stock.getAvailableQty();
                int beforeLocked = stock.getLockedQty();
                stock.lock(alloc.quantity());
                stockRepository.save(stock);

                String txId = IdGenerator.nextIdStr();
                transactionRepository.append(new StockTransaction(txId, cmd.bizNo(), TransactionType.LOCK,
                        alloc.warehouseId(), alloc.skuId(), alloc.quantity(),
                        beforeQty, stock.getAvailableQty(), beforeLocked, stock.getLockedQty(),
                        "下单预占", LocalDateTime.now()));
                if (firstTxId == null) {
                    firstTxId = txId;
                    primaryWarehouse = alloc.warehouseId();
                }
            }
        }
        if (firstTxId != null) {
            pendingEvents.add(new StockLockedEvent(primaryWarehouse, cmd.bizNo(), firstTxId));
        }
        return firstTxId;
    }

    // ============================================================
    // 用例 2：实扣库存 (支付成功)
    // ============================================================

    /**
     * 实扣库存：把当初 LOCK 的预占转为实际售出（locked -= qty）。
     * <p>依赖 LOCK 流水明细逐条实扣，天然支持一单多仓拆分。</p>
     */
    public StockOperationResult deduct(String bizNo) {
        validateBizNo(bizNo);
        // 幂等校验移入业务号锁保护的事务内。
        List<DomainEvent> pendingEvents = new ArrayList<>();
        String transactionId = executeWithRetry("deduct", bizNo, () -> {
            pendingEvents.clear();
            if (transactionRepository.existsByBizNoAndType(bizNo, TransactionType.DEDUCT))
                return firstTransactionId(bizNo, TransactionType.DEDUCT);
            var locks = transactionRepository.findByBizNoAndType(bizNo, TransactionType.LOCK);
            if (locks.isEmpty()) throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED, "未找到预占流水");
            return doDeduct(bizNo, locks, pendingEvents);
        });
        publishAfterCommit(pendingEvents);
        log.info("[Inventory] deduct success bizNo={} txId={}", bizNo, transactionId);
        return StockOperationResult.ok(bizNo, transactionId, false);
    }

    private String doDeduct(String bizNo, List<StockTransaction> locks, List<DomainEvent> pendingEvents) {
        String firstTxId = null;
        String primaryWarehouse = null;
        for (StockTransaction lock : locks) {
            if (transactionRepository.findIdempotent(bizNo, TransactionType.DEDUCT,
                    lock.getWarehouseId(), lock.getSkuId()).isPresent()) {
                continue;
            }
            Stock stock = loadStock(lock.getWarehouseId(), lock.getSkuId());
            int beforeLocked = stock.getLockedQty();
            stock.deduct(lock.getQuantity());
            stockRepository.save(stock);

            String txId = IdGenerator.nextIdStr();
            transactionRepository.append(new StockTransaction(txId, bizNo, TransactionType.DEDUCT,
                    lock.getWarehouseId(), lock.getSkuId(), lock.getQuantity(),
                    stock.getAvailableQty(), stock.getAvailableQty(), beforeLocked, stock.getLockedQty(),
                    "支付成功实扣", LocalDateTime.now()));
            if (firstTxId == null) {
                firstTxId = txId;
                primaryWarehouse = lock.getWarehouseId();
            }
        }
        if (firstTxId != null) {
            pendingEvents.add(new StockDeductedEvent(primaryWarehouse, bizNo, firstTxId));
        }
        return firstTxId;
    }

    // ============================================================
    // 用例 3：释放预占 (取消 / 超时)
    // ============================================================

    /**
     * 释放预占：把 LOCK 的库存归还给可用（locked -= qty, available += qty）。
     */
    public StockOperationResult release(String bizNo, String reason) {
        validateBizNo(bizNo);
        // 释放即使先于预占到达，也必须写入取消屏障。
        // 没有预占流水，视为无需释放，幂等返回成功

        String finalReason = (reason == null || reason.isBlank()) ? "订单取消释放" : reason;
        List<DomainEvent> pendingEvents = new ArrayList<>();
        String transactionId = executeWithRetry("release", bizNo, () -> {
            pendingEvents.clear();
            if (transactionRepository.existsByBizNoAndType(bizNo, TransactionType.UNLOCK))
                return firstTransactionId(bizNo, TransactionType.UNLOCK);
            var locks = transactionRepository.findByBizNoAndType(bizNo, TransactionType.LOCK);
            return doRelease(bizNo, locks, finalReason, pendingEvents);
        });
        publishAfterCommit(pendingEvents);
        log.info("[Inventory] release success bizNo={} reason={}", bizNo, finalReason);
        return StockOperationResult.ok(bizNo, transactionId, false);
    }

    private String doRelease(String bizNo, List<StockTransaction> locks, String reason, List<DomainEvent> pendingEvents) {
        String firstTxId = null;
        String primaryWarehouse = null;
        for (StockTransaction lock : locks) {
            if (transactionRepository.findIdempotent(bizNo, TransactionType.UNLOCK,
                    lock.getWarehouseId(), lock.getSkuId()).isPresent()) {
                continue;
            }
            Stock stock = loadStock(lock.getWarehouseId(), lock.getSkuId());
            int beforeQty = stock.getAvailableQty();
            int beforeLocked = stock.getLockedQty();
            stock.unlock(lock.getQuantity());
            stockRepository.save(stock);

            String txId = IdGenerator.nextIdStr();
            transactionRepository.append(new StockTransaction(txId, bizNo, TransactionType.UNLOCK,
                    lock.getWarehouseId(), lock.getSkuId(), lock.getQuantity(),
                    beforeQty, stock.getAvailableQty(), beforeLocked, stock.getLockedQty(),
                    reason, LocalDateTime.now()));
            if (firstTxId == null) {
                firstTxId = txId;
                primaryWarehouse = lock.getWarehouseId();
            }
        }
        if (firstTxId != null) {
            pendingEvents.add(new StockReleasedEvent(primaryWarehouse, bizNo, reason));
        }
        return firstTxId;
    }

    // ============================================================
    // 用例 4：人工调整 (盘点 / 损耗 / 入库)
    // ============================================================

    public void adjust(AdjustStockCommand cmd) {
        List<DomainEvent> pendingEvents = new ArrayList<>();
        executeWithRetry("adjust", cmd.warehouseId() + ":" + cmd.skuId(), () -> {
            Stock stock = loadStock(cmd.warehouseId(), cmd.skuId());
            int beforeQty = stock.getAvailableQty();
            int beforeLocked = stock.getLockedQty();
            stock.adjust(cmd.delta(), cmd.reason());
            stockRepository.save(stock);
            transactionRepository.append(new StockTransaction(IdGenerator.nextIdStr(),
                    "ADJUST-" + IdGenerator.nextIdStr(), TransactionType.ADJUST,
                    cmd.warehouseId(), cmd.skuId(), Math.abs(cmd.delta()),
                    beforeQty, stock.getAvailableQty(), beforeLocked, stock.getLockedQty(),
                    cmd.reason(), LocalDateTime.now()));
            return null;
        });
        publishAfterCommit(pendingEvents);
        log.info("[Inventory] adjust warehouseId={} skuId={} delta={}", cmd.warehouseId(), cmd.skuId(), cmd.delta());
    }

    // ============================================================
    // 用例 5：查询
    // ============================================================

    public Optional<Stock> getStock(String warehouseId, String skuId) {
        return stockRepository.findByWarehouseAndSku(warehouseId, skuId);
    }

    public List<Stock> listBySku(String skuId) {
        return stockRepository.findBySku(skuId);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    /**
     * 乐观锁重试包装：在独立事务中执行 action，冲突则重试，最多 maxRetry 次。
     */
    private <T> T executeWithRetry(String opName, String bizNo, java.util.function.Supplier<T> action) {
        ObjectOptimisticLockingFailureException lastError = null;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                return transactionTemplate.execute(status -> {
                    String state = operationGuard.lock(bizNo);
                    if ("DEDUCTED".equals(state) && "release".equals(opName)) return null;
                    if ("RELEASED".equals(state) && !"release".equals(opName)) {
                        throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED, "库存业务状态不允许此操作: " + state);
                    }
                    T result = action.get();
                    String next = switch (opName) {
                        case "lock" -> "DEDUCTED".equals(state) ? state : "LOCKED";
                        case "deduct" -> "DEDUCTED";
                        case "release" -> "RELEASED";
                        default -> state;
                    };
                    operationGuard.state(bizNo, next);
                    return result;
                });
            } catch (ObjectOptimisticLockingFailureException e) {
                lastError = e;
                log.warn("[Inventory] {} optimistic lock conflict bizNo={} attempt={}/{}",
                        opName, bizNo, attempt, maxRetry);
            }
        }
        throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED,
                "库存操作并发冲突，重试 " + maxRetry + " 次仍失败: " + bizNo, lastError);
    }

    private void publishAfterCommit(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.error("[Inventory] publish event failed: {}", event, e);
            }
        }
        events.clear();
    }

    private Stock loadStock(String warehouseId, String skuId) {
        return stockRepository.findByWarehouseAndSku(warehouseId, skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_STOCK_NOT_FOUND,
                        String.format("库存记录不存在: warehouseId=%s skuId=%s", warehouseId, skuId)));
    }

    private String firstTransactionId(String bizNo, TransactionType type) {
        return transactionRepository.findByBizNoAndType(bizNo, type).stream()
                .findFirst().map(StockTransaction::getTransactionId).orElse(null);
    }

    private void validateBizNo(String bizNo) {
        if (bizNo == null || bizNo.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "bizNo 不能为空");
        }
    }
}
