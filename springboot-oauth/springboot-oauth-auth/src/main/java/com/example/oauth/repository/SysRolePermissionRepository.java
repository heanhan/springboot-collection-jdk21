package com.example.oauth.repository;

import com.example.oauth.entity.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-权限关联 Repository
 */
@Repository
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, SysRolePermission.PK> {

    /**
     * 根据角色 ID 查询关联记录
     */
    List<SysRolePermission> findByRoleId(Long roleId);

    /**
     * 根据权限 ID 查询关联记录
     */
    List<SysRolePermission> findByPermissionId(Long permissionId);

    /**
     * 删除指定角色的全部权限关联（重新分配权限前清理 / 删除角色时清理）
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除指定权限的全部角色关联（删除权限时级联清理）
     */
    void deleteByPermissionId(Long permissionId);
}
