package com.example.ddd.inventory.domain.service;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.inventory.domain.model.aggregate.Stock;
import com.example.ddd.inventory.domain.model.aggregate.Warehouse;
import com.example.ddd.inventory.domain.repository.StockRepository;
import com.example.ddd.inventory.domain.repository.WarehouseRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 领域服务：库存预占分配策略 (Stock Reservation)。
 *
 * <p><b>为什么是领域服务而不是聚合根方法？</b>
 * 分配策略需要<b>跨多个 Stock 聚合</b>（甚至跨仓库）协调，还要读取 Warehouse 的优先级，
 * 这超出了单个 Stock 聚合的职责边界。按 DDD 规则，"跨聚合、需要外部信息的业务逻辑"应放入领域服务。</p>
 *
 * <p><b>策略：</b>
 * <ol>
 *   <li>若调用方指定了 warehouseId，则单仓分配：该仓不足直接判"库存不足"。</li>
 *   <li>否则按仓库 priority 升序（数字越小越优先）贪心拆分，直到凑齐所需数量；
 *       仍不足则抛 {@link ErrorCode#INVENTORY_STOCK_SHORTAGE}。</li>
 * </ol>
 * 这是"就近 / 优先级发货"的经典实现，支持一单多仓拆分配。</p>
 *
 * <p><b>无 Spring 依赖：</b>本类不加 {@code @Service}，由 infrastructure 层的
 * {@code DomainServiceConfig} 显式装配为 Bean，保持领域层纯粹。</p>
 */
public class StockReservationService {

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;

    public StockReservationService(StockRepository stockRepository,
                                   WarehouseRepository warehouseRepository) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * 为一个 SKU 计算预占分配方案。
     *
     * @param skuId              SKU ID
     * @param requiredQty        需要的数量（&gt; 0）
     * @param preferredWarehouseId 指定仓库（可为 null，表示自动分配）
     * @return 分配明细列表，总数量 == requiredQty
     * @throws BusinessException 库存不足或库存记录不存在
     */
    public List<Allocation> allocate(String skuId, int requiredQty, String preferredWarehouseId) {
        if (requiredQty <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预占数量必须大于 0");
        }

        // 场景一：调用方指定仓库，单仓分配
        if (preferredWarehouseId != null && !preferredWarehouseId.isBlank()) {
            Stock stock = stockRepository.findByWarehouseAndSku(preferredWarehouseId, skuId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_STOCK_NOT_FOUND,
                            String.format("库存记录不存在: warehouseId=%s skuId=%s", preferredWarehouseId, skuId)));
            if (stock.getAvailableQty() < requiredQty) {
                throw new BusinessException(ErrorCode.INVENTORY_STOCK_SHORTAGE,
                        String.format("指定仓库库存不足: skuId=%s 需要=%d 可用=%d", skuId, requiredQty, stock.getAvailableQty()));
            }
            return List.of(new Allocation(preferredWarehouseId, skuId, requiredQty));
        }

        // 场景二：按仓库优先级贪心拆分
        List<Warehouse> warehouses = warehouseRepository.findAllEnabled().stream()
                .sorted(Comparator.comparingInt(Warehouse::getPriority))
                .toList();

        List<Allocation> allocations = new ArrayList<>();
        int remaining = requiredQty;
        for (Warehouse warehouse : warehouses) {
            if (remaining <= 0) {
                break;
            }
            Optional<Stock> stockOpt = stockRepository.findByWarehouseAndSku(warehouse.getWarehouseId(), skuId);
            if (stockOpt.isEmpty()) {
                continue;
            }
            int canTake = Math.min(stockOpt.get().getAvailableQty(), remaining);
            if (canTake > 0) {
                allocations.add(new Allocation(warehouse.getWarehouseId(), skuId, canTake));
                remaining -= canTake;
            }
        }

        if (remaining > 0) {
            throw new BusinessException(ErrorCode.INVENTORY_STOCK_SHORTAGE,
                    String.format("全仓库存不足: skuId=%s 需要=%d 缺口=%d", skuId, requiredQty, remaining));
        }
        return allocations;
    }

    /**
     * 值对象：一次分配结果。
     *
     * <p><b>为什么是值对象？</b>无独立标识、不可变、按值相等——它只是"从哪个仓拿多少个"的数据袋。</p>
     *
     * @param warehouseId 仓库 ID
     * @param skuId       SKU ID
     * @param quantity    分配数量（&gt; 0）
     */
    public record Allocation(String warehouseId, String skuId, int quantity) {
    }
}
