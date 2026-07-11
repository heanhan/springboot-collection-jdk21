package com.example.oauth.repository;

import com.example.oauth.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统菜单 Repository
 */
@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {

    /**
     * 根据用户 ID 查询该用户拥有的所有菜单（通过角色关联）
     * （sys_menu -> sys_role_menu -> sys_user_role）
     */
    @Query(value = "SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = :userId AND m.status = 1 " +
            "ORDER BY m.sort ASC", nativeQuery = true)
    List<SysMenu> findMenusByUserId(@Param("userId") Long userId);

    /**
     * 根据角色 ID 查询菜单列表（sys_menu -> sys_role_menu）
     */
    @Query(value = "SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "WHERE rm.role_id = :roleId AND m.status = 1 " +
            "ORDER BY m.sort ASC", nativeQuery = true)
    List<SysMenu> findMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询所有启用的菜单
     */
    @Query("SELECT m FROM SysMenu m WHERE m.status = 1 ORDER BY m.sort ASC")
    List<SysMenu> findAllEnabled();

    /**
     * 查询子菜单（删除前校验是否存在下级）
     */
    List<SysMenu> findByParentId(Long parentId);
}
