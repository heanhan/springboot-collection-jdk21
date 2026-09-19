package com.example.ddd.user.domain.repository;

import com.example.ddd.user.domain.model.aggregate.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 仓储接口：Permission 聚合根。
 */
public interface PermissionRepository {

    Optional<Permission> findById(String permissionId);

    Optional<Permission> findByCode(String permissionCode);

    List<Permission> findByIds(Set<String> permissionIds);

    List<Permission> findAll();

    void save(Permission permission);
}
