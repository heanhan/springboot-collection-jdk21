package com.example.ddd.inventory.infrastructure.persistence.dao;

import com.example.ddd.inventory.infrastructure.persistence.po.StockPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_stock。
 */
public interface StockDao extends JpaRepository<StockPO, String> {

    @Query("SELECT s FROM StockPO s WHERE s.stockId = :stockId AND s.deleted = 0")
    Optional<StockPO> findByIdAndNotDeleted(@Param("stockId") String stockId);

    @Query("SELECT s FROM StockPO s WHERE s.warehouseId = :warehouseId AND s.skuId = :skuId AND s.deleted = 0")
    Optional<StockPO> findByWarehouseAndSku(@Param("warehouseId") String warehouseId, @Param("skuId") String skuId);

    @Query("SELECT s FROM StockPO s WHERE s.skuId = :skuId AND s.deleted = 0")
    List<StockPO> findBySkuId(@Param("skuId") String skuId);
}
