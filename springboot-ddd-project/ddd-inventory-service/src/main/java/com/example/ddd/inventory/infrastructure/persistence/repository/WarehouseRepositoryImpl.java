package com.example.ddd.inventory.infrastructure.persistence.repository;

import com.example.ddd.inventory.domain.model.aggregate.Warehouse;
import com.example.ddd.inventory.domain.repository.WarehouseRepository;
import com.example.ddd.inventory.infrastructure.persistence.converter.InventoryConverter;
import com.example.ddd.inventory.infrastructure.persistence.dao.WarehouseDao;
import com.example.ddd.inventory.infrastructure.persistence.po.WarehousePO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Warehouse 聚合。
 */
@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final WarehouseDao warehouseDao;

    public WarehouseRepositoryImpl(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findById(String warehouseId) {
        return warehouseDao.findByIdAndNotDeleted(warehouseId).map(InventoryConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findByCode(String code) {
        return warehouseDao.findByCode(code).map(InventoryConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return warehouseDao.findByCode(code).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllEnabled() {
        return warehouseDao.findAllEnabled().stream().map(InventoryConverter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Warehouse warehouse) {
        WarehousePO po = InventoryConverter.toPO(warehouse);
        warehouseDao.findByIdAndNotDeleted(warehouse.getWarehouseId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        warehouseDao.save(po);
    }
}
