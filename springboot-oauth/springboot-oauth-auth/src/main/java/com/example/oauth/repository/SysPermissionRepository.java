package com.example.oauth.repository;

import com.example.oauth.entity.SysPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统权限 Repository
 */
@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {

    /**
     * 根据用户 ID 查询该用户拥有的所有权限编码
     * （sys_permission -> sys_role_permission -> sys_user_role）
     */
    @Query(value = "SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "INNER JOIN sys_role r ON r.id = rp.role_id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id " +
            "WHERE ur.user_id = :userId AND p.status = 1 AND r.status = 1", nativeQuery = true)
    List<String> findPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色 ID 查询权限列表（sys_permission -> sys_role_permission）
     */
    @Query(value = "SELECT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = :roleId AND p.status = 1", nativeQuery = true)
    List<SysPermission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * 按类型和状态查询权限（用于动态鉴权加载接口权限：type=3 接口，status=1 启用）
     */
    List<SysPermission> findByTypeAndStatus(Integer type, Integer status);

    /**
     * 按权限编码查询
     */
    SysPermission findByPermCode(String permCode);

    /**
     * 权限编码是否存在
     */
    boolean existsByPermCode(String permCode);

    /**
     * 按权限名称模糊分页查询
     */
    Page<SysPermission> findByPermNameContaining(String permName, Pageable pageable);
}
