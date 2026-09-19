package com.example.ddd.inventory.domain.repository;

import com.example.ddd.inventory.domain.model.aggregate.Stock;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Stock 聚合。
 */
public interface StockRepository {

    Optional<Stock> findById(String stockId);

    Optional<Stock> findByWarehouseAndSku(String warehouseId, String skuId);

    /** 查询某个 SKU 在所有仓库的库存记录（供跨仓库分配策略使用）。 */
    List<Stock> findBySku(String skuId);

    void save(Stock stock);
}
