package com.example.ddd.user.interfaces.rest;

import com.example.ddd.common.result.Result;
import com.example.ddd.user.application.service.RbacQueryService;
import com.example.ddd.user.domain.model.aggregate.Permission;
import com.example.ddd.user.domain.model.aggregate.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * REST 控制器：RBAC 查询（角色、权限）。
 */
@RestController
@RequestMapping("/rbac")
public class RbacController {

    private final RbacQueryService rbacQueryService;

    public RbacController(RbacQueryService rbacQueryService) {
        this.rbacQueryService = rbacQueryService;
    }

    @GetMapping("/roles")
    public Result<List<RoleSummary>> listRoles() {
        List<Role> roles = rbacQueryService.listAllRoles();
        return Result.ok(roles.stream().map(RoleSummary::from).toList());
    }

    @GetMapping("/permissions")
    public Result<List<PermissionSummary>> listPermissions() {
        List<Permission> permissions = rbacQueryService.listAllPermissions();
        return Result.ok(permissions.stream().map(PermissionSummary::from).toList());
    }

    @GetMapping("/users/{userId}/roles")
    public Result<Set<String>> getUserRoles(@PathVariable String userId) {
        return Result.ok(rbacQueryService.findRoleCodesByUserId(userId));
    }

    @GetMapping("/users/{userId}/permissions")
    public Result<Set<String>> getUserPermissions(@PathVariable String userId) {
        return Result.ok(rbacQueryService.findPermissionCodesByUserId(userId));
    }

    /**
     * 简化的角色摘要 DTO
     */
    public record RoleSummary(String roleId, String roleCode, String roleName, String description, boolean active) {
        static RoleSummary from(Role role) {
            return new RoleSummary(role.getRoleId(), role.getRoleCode(), role.getRoleName(),
                    role.getDescription(), role.isActive());
        }
    }

    /**
     * 简化的权限摘要 DTO
     */
    public record PermissionSummary(String permissionId, String permissionCode, String permissionName,
                                    String resource, String action, String description) {
        static PermissionSummary from(Permission p) {
            return new PermissionSummary(p.getPermissionId(), p.getPermissionCode(), p.getPermissionName(),
                    p.getResource(), p.getAction(), p.getDescription());
        }
    }
}
