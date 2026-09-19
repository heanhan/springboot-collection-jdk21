package com.example.ddd.product.infrastructure.persistence.dao;

import com.example.ddd.product.infrastructure.persistence.po.SkuPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_sku。
 */
public interface SkuDao extends JpaRepository<SkuPO, String> {

    @Query("SELECT s FROM SkuPO s WHERE s.skuId = :skuId AND s.deleted = 0")
    Optional<SkuPO> findByIdAndNotDeleted(@Param("skuId") String skuId);

    @Query("SELECT s FROM SkuPO s WHERE s.spuId = :spuId AND s.deleted = 0")
    List<SkuPO> findBySpuId(@Param("spuId") String spuId);

    @Query("SELECT s FROM SkuPO s WHERE s.skuId IN :skuIds AND s.deleted = 0")
    List<SkuPO> findByIdIn(@Param("skuIds") List<String> skuIds);

    @Modifying
    @Query("UPDATE SkuPO s SET s.deleted = 1 WHERE s.skuId = :skuId")
    int softDelete(@Param("skuId") String skuId);
}
