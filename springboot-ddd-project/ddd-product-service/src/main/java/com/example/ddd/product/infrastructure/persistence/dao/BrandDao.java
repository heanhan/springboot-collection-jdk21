package com.example.ddd.product.infrastructure.persistence.dao;

import com.example.ddd.product.infrastructure.persistence.po.BrandPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_brand。
 */
public interface BrandDao extends JpaRepository<BrandPO, String> {

    @Query("SELECT b FROM BrandPO b WHERE b.brandId = :id AND b.deleted = 0")
    Optional<BrandPO> findByIdAndNotDeleted(@Param("id") String id);

    @Query("SELECT b FROM BrandPO b WHERE b.name = :name AND b.deleted = 0")
    Optional<BrandPO> findByName(@Param("name") String name);

    @Query("SELECT COUNT(b) > 0 FROM BrandPO b WHERE b.name = :name AND b.deleted = 0")
    boolean existsByNameNotDeleted(@Param("name") String name);

    @Query("SELECT b FROM BrandPO b WHERE b.deleted = 0 AND b.status = 'ACTIVE' ORDER BY b.sort ASC")
    List<BrandPO> findAllEnabled();
}
