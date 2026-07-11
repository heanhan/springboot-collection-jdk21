package com.example.oauth.repository;

import com.example.oauth.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-角色关联 Repository
 */
@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, SysUserRole.PK> {

    /**
     * 根据用户 ID 查询关联记录
     */
    List<SysUserRole> findByUserId(Long userId);

    /**
     * 根据角色 ID 查询关联记录
     */
    List<SysUserRole> findByRoleId(Long roleId);

    /**
     * 删除指定用户的全部角色关联（重新分配角色前清理）
     */
    void deleteByUserId(Long userId);

    /**
     * 删除指定角色的全部用户关联（删除角色时级联清理）
     */
    void deleteByRoleId(Long roleId);
}
