package com.example.oauth.service;

import com.example.oauth.entity.SysRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 角色管理服务（RBAC 管理接口）
 */
public interface SysRoleAdminService {

    /** 分页查询角色（keyword 按角色名模糊，可空） */
    Page<SysRole> page(String keyword, Pageable pageable);

    /** 查询全部角色（用于下拉选择） */
    List<SysRole> listAll();

    /** 按 ID 查询角色 */
    SysRole getById(Long id);

    /** 新增角色 */
    SysRole create(SysRole role);

    /** 更新角色 */
    SysRole update(SysRole role);

    /** 删除角色（同时清理用户/权限/菜单关联） */
    void delete(Long id);

    /** 给角色分配权限（全量覆盖） */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /** 查询角色当前拥有的权限 ID 列表 */
    List<Long> getPermissionIds(Long roleId);

    /** 给角色分配菜单（全量覆盖） */
    void assignMenus(Long roleId, List<Long> menuIds);

    /** 查询角色当前拥有的菜单 ID 列表 */
    List<Long> getMenuIds(Long roleId);
}
