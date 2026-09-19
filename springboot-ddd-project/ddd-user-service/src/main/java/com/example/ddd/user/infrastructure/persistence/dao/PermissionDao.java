package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.PermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * DAO：权限。
 */
public interface PermissionDao extends JpaRepository<PermissionPO, String> {

    @Query("SELECT p FROM PermissionPO p WHERE p.permissionCode = :code AND p.deleted = 0")
    Optional<PermissionPO> findByCode(@Param("code") String code);

    @Query("SELECT p FROM PermissionPO p WHERE p.permissionId IN :ids AND p.deleted = 0")
    List<PermissionPO> findByIdIn(@Param("ids") Collection<String> ids);

    @Query("SELECT p FROM PermissionPO p WHERE p.deleted = 0 ORDER BY p.permissionCode ASC")
    List<PermissionPO> findAllNotDeleted();
}
