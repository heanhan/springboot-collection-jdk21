package com.example.oauth.repository;

import com.example.oauth.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统用户 Repository
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 根据用户名查询用户（仅用户基本信息，角色/权限另行查询）
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 根据用户 ID 查询其启用的角色编码（通过 sys_user_role 关联表）
     */
    @Query(value = "SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = :userId AND r.status = 1", nativeQuery = true)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 按用户名模糊分页查询
     */
    Page<SysUser> findByUsernameContaining(String username, Pageable pageable);
}
