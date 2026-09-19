package com.example.ddd.inventory.infrastructure.persistence.dao;

import com.example.ddd.inventory.infrastructure.persistence.po.WarehousePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_warehouse。
 */
public interface WarehouseDao extends JpaRepository<WarehousePO, String> {

    @Query("SELECT w FROM WarehousePO w WHERE w.warehouseId = :warehouseId AND w.deleted = 0")
    Optional<WarehousePO> findByIdAndNotDeleted(@Param("warehouseId") String warehouseId);

    @Query("SELECT w FROM WarehousePO w WHERE w.code = :code AND w.deleted = 0")
    Optional<WarehousePO> findByCode(@Param("code") String code);

    @Query("SELECT w FROM WarehousePO w WHERE w.deleted = 0 AND w.status = 'ACTIVE' ORDER BY w.priority ASC")
    List<WarehousePO> findAllEnabled();
}
