package com.example.oauth.service;

import com.example.oauth.entity.SysPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 权限管理服务（RBAC 管理接口）
 * <p>
 * 权限（尤其是 type=3 接口权限）的增删改都会影响动态鉴权规则，
 * 因此实现中需在变更后刷新 {@code DynamicPermissionCache}。
 */
public interface SysPermissionAdminService {

    /** 分页查询权限（keyword 按权限名模糊，可空） */
    Page<SysPermission> page(String keyword, Pageable pageable);

    /** 查询全部权限（用于下拉/分配选择） */
    List<SysPermission> listAll();

    /** 按 ID 查询权限 */
    SysPermission getById(Long id);

    /** 新增权限 */
    SysPermission create(SysPermission permission);

    /** 更新权限 */
    SysPermission update(SysPermission permission);

    /** 删除权限（同时清理角色关联并刷新鉴权规则） */
    void delete(Long id);
}
