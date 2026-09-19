package com.example.ddd.inventory.domain.repository;

import com.example.ddd.inventory.domain.model.aggregate.Warehouse;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Warehouse 聚合。
 */
public interface WarehouseRepository {

    Optional<Warehouse> findById(String warehouseId);

    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);

    List<Warehouse> findAllEnabled();

    void save(Warehouse warehouse);
}
