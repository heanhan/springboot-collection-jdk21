package com.example.oauth.service;

import com.example.oauth.dto.UserSaveDTO;
import com.example.oauth.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 用户管理服务（RBAC 管理接口）
 */
public interface SysUserAdminService {

    /** 分页查询用户（keyword 按用户名模糊，可空） */
    Page<SysUser> page(String keyword, Pageable pageable);

    /** 按 ID 查询用户 */
    SysUser getById(Long id);

    /** 新增用户 */
    SysUser create(UserSaveDTO dto);

    /** 更新用户 */
    SysUser update(UserSaveDTO dto);

    /** 删除用户（同时清理角色关联） */
    void delete(Long id);

    /** 修改用户状态（0-禁用 1-启用） */
    void changeStatus(Long id, Integer status);

    /** 重置用户密码 */
    void resetPassword(Long id, String newPassword);

    /** 给用户分配角色（全量覆盖） */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 查询用户当前拥有的角色 ID 列表 */
    List<Long> getRoleIds(Long userId);
}
