package com.example.ddd.user.infrastructure.persistence.dao;

import com.example.ddd.user.infrastructure.persistence.po.RolePermissionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * DAO：角色-权限关联。
 */
public interface RolePermissionDao extends JpaRepository<RolePermissionPO, Long> {

    @Query("SELECT rp FROM RolePermissionPO rp WHERE rp.roleId IN :roleIds")
    List<RolePermissionPO> findByRoleIdIn(@Param("roleIds") Collection<String> roleIds);
}
