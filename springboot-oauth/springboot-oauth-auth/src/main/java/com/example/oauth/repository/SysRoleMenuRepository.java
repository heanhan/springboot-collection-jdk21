package com.example.oauth.repository;

import com.example.oauth.entity.SysRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-菜单关联 Repository
 */
@Repository
public interface SysRoleMenuRepository extends JpaRepository<SysRoleMenu, SysRoleMenu.PK> {

    /**
     * 根据角色 ID 查询关联记录
     */
    List<SysRoleMenu> findByRoleId(Long roleId);

    /**
     * 根据菜单 ID 查询关联记录
     */
    List<SysRoleMenu> findByMenuId(Long menuId);

    /**
     * 删除指定角色的全部菜单关联（重新分配菜单前清理 / 删除角色时清理）
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除指定菜单的全部角色关联（删除菜单时级联清理）
     */
    void deleteByMenuId(Long menuId);
}
