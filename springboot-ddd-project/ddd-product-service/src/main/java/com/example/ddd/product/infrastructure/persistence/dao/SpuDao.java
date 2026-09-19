package com.example.ddd.product.infrastructure.persistence.dao;

import com.example.ddd.product.infrastructure.persistence.po.SpuPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_spu。
 */
public interface SpuDao extends JpaRepository<SpuPO, String> {

    @Query("SELECT p FROM SpuPO p WHERE p.spuId = :spuId AND p.deleted = 0")
    Optional<SpuPO> findByIdAndNotDeleted(@Param("spuId") String spuId);

    @Query("SELECT p FROM SpuPO p WHERE p.deleted = 0 "
            + "AND (:categoryId IS NULL OR p.categoryId = :categoryId) "
            + "AND (:status IS NULL OR p.status = :status) "
            + "ORDER BY p.createTime DESC")
    List<SpuPO> findByFilter(@Param("categoryId") String categoryId,
                             @Param("status") String status,
                             Pageable pageable);

    @Query("SELECT COUNT(p) FROM SpuPO p WHERE p.deleted = 0 "
            + "AND (:categoryId IS NULL OR p.categoryId = :categoryId) "
            + "AND (:status IS NULL OR p.status = :status)")
    long countByFilter(@Param("categoryId") String categoryId, @Param("status") String status);

    @Query("SELECT p FROM SpuPO p WHERE p.deleted = 0 AND p.status = 'ON_SALE' "
            + "AND (:categoryId IS NULL OR p.categoryId = :categoryId) "
            + "ORDER BY p.salesCount DESC, p.createTime DESC")
    List<SpuPO> findOnSale(@Param("categoryId") String categoryId, Pageable pageable);
}
