package com.example.ddd.user.application.service;

import com.example.ddd.user.domain.model.aggregate.Permission;
import com.example.ddd.user.domain.model.aggregate.Role;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.repository.PermissionRepository;
import com.example.ddd.user.domain.repository.RoleRepository;
import com.example.ddd.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用服务：RBAC 查询用例。
 *
 * <p>本服务负责回答"用户拥有哪些角色/权限"，被 auth-service 通过 Feign 调用，
 * 用于登录时构造 JWT payload 中的 permissions/roles 字段。</p>
 *
 * <p><b>为什么查询逻辑也放在应用服务？</b>
 * CQRS 简化版：命令 (Command) 走聚合根 + 仓储；查询 (Query) 直接走仓储，
 * 无需构造完整聚合。生产环境可拆分独立的 QueryService，直接返回 DTO。</p>
 *
 * @author ddd-learning
 */
@Service
public class RbacQueryService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacQueryService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * 查询用户的角色码集合（例如 ["USER", "ADMIN"]）。
     */
    @Transactional(readOnly = true)
    public Set<String> findRoleCodesByUserId(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleIds().isEmpty()) {
            return Collections.emptySet();
        }
        List<Role> roles = roleRepository.findByIds(new HashSet<>(user.getRoleIds()));
        return roles.stream().filter(Role::isActive).map(Role::getRoleCode).collect(Collectors.toSet());
    }

    /**
     * 查询用户的权限码集合（角色权限并集）。
     */
    @Transactional(readOnly = true)
    public Set<String> findPermissionCodesByUserId(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoleIds().isEmpty()) {
            return Collections.emptySet();
        }
        List<Role> roles = roleRepository.findByIds(new HashSet<>(user.getRoleIds()));
        Set<String> permissionIds = roles.stream()
                .filter(Role::isActive)
                .flatMap(r -> r.getPermissionIds().stream())
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Permission> permissions = permissionRepository.findByIds(permissionIds);
        return permissions.stream().map(Permission::getPermissionCode).collect(Collectors.toSet());
    }

    /**
     * 查询所有角色（管理端）。
     */
    @Transactional(readOnly = true)
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 查询所有权限（管理端）。
     */
    @Transactional(readOnly = true)
    public List<Permission> listAllPermissions() {
        return permissionRepository.findAll();
    }
}
