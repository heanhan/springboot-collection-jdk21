package com.example.oauth.service;

import com.example.oauth.entity.SysMenu;

import java.util.List;

/**
 * 菜单管理服务（RBAC 管理接口）
 */
public interface SysMenuAdminService {

    /** 查询全部菜单（平铺列表，按 sort 升序） */
    List<SysMenu> listAll();

    /** 查询菜单树（含 children） */
    List<SysMenu> tree();

    /** 按 ID 查询菜单 */
    SysMenu getById(Long id);

    /** 新增菜单 */
    SysMenu create(SysMenu menu);

    /** 更新菜单 */
    SysMenu update(SysMenu menu);

    /** 删除菜单（存在子菜单时禁止删除，同时清理角色关联） */
    void delete(Long id);
}
