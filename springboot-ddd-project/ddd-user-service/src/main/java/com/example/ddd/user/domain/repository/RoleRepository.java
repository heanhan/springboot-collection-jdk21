package com.example.ddd.user.domain.repository;

import com.example.ddd.user.domain.model.aggregate.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 仓储接口：Role 聚合根。
 */
public interface RoleRepository {

    Optional<Role> findById(String roleId);

    Optional<Role> findByCode(String roleCode);

    List<Role> findByIds(Set<String> roleIds);

    List<Role> findAll();

    void save(Role role);
}
