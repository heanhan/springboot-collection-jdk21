package com.example.ddd.product.infrastructure.persistence.dao;

import com.example.ddd.product.infrastructure.persistence.po.CategoryPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA DAO：t_category。
 */
public interface CategoryDao extends JpaRepository<CategoryPO, String> {

    @Query("SELECT c FROM CategoryPO c WHERE c.categoryId = :id AND c.deleted = 0")
    Optional<CategoryPO> findByIdAndNotDeleted(@Param("id") String id);

    @Query("SELECT c FROM CategoryPO c WHERE c.parentId = :parentId AND c.deleted = 0 ORDER BY c.sort ASC")
    List<CategoryPO> findByParentId(@Param("parentId") String parentId);

    @Query("SELECT c FROM CategoryPO c WHERE c.codePath LIKE CONCAT(:prefix, '%') AND c.deleted = 0 ORDER BY c.codePath")
    List<CategoryPO> findSubtree(@Param("prefix") String prefix);

    @Query("SELECT c FROM CategoryPO c WHERE c.deleted = 0 AND c.status = 'ACTIVE' ORDER BY c.level, c.sort")
    List<CategoryPO> findAllEnabled();
}
