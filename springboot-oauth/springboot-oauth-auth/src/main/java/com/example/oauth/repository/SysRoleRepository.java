package com.example.oauth.repository;

import com.example.oauth.entity.SysRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统角色 Repository
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    Optional<SysRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    /**
     * 按角色名称模糊分页查询
     */
    Page<SysRole> findByRoleNameContaining(String roleName, Pageable pageable);
}
