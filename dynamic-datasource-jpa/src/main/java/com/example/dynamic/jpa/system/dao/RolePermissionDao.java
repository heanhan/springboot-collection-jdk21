package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionDao extends JpaRepository<RolePermission, Integer> {

    List<RolePermission> findByRoleId(Integer roleId);

    List<RolePermission> findByRoleIdIn(List<Integer> roleIds);

    void deleteByRoleId(Integer roleId);
}
