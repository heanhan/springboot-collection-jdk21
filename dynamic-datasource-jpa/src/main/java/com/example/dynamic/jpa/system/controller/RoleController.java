package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.service.RoleService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping
    public ResultBody list(@RequestParam(required = false) Integer tenantId) {
        Integer currentTenant = currentTenant();
        if (currentTenant != SystemConstant.SYSTEM_TENANT_ID) {
            tenantId = currentTenant;
        }
        if (tenantId == null) {
            throw new BaseException("平台管理员查询角色时必须指定租户id");
        }
        return ResultBody.success(roleService.listRoles(tenantId));
    }

    @PostMapping
    public ResultBody add(@Valid @RequestBody Role role) {
        role.setTenantId(resolveTenant(role.getTenantId()));
        return ResultBody.success(roleService.addRole(role));
    }

    @PutMapping("/{id}")
    public ResultBody update(@PathVariable Integer id, @Valid @RequestBody Role role) {
        Role existing = requireScopedRole(id);
        role.setId(id);
        role.setTenantId(existing.getTenantId());
        return ResultBody.success(roleService.updateRole(role));
    }

    @PutMapping("/{id}/permissions")
    public ResultBody permissions(@PathVariable Integer id, @RequestBody List<String> permissionCodes) {
        requireScopedRole(id);
        roleService.replaceRolePermissions(id, permissionCodes);
        return ResultBody.success("权限分配成功");
    }

    @DeleteMapping("/{id}")
    public ResultBody delete(@PathVariable Integer id) {
        requireScopedRole(id);
        return ResultBody.success(roleService.deleteRole(id));
    }

    private Role requireScopedRole(Integer id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            throw new BaseException("角色不存在");
        }
        resolveTenant(role.getTenantId());
        return role;
    }

    private Integer resolveTenant(Integer requested) {
        Integer currentTenant = currentTenant();
        if (currentTenant != SystemConstant.SYSTEM_TENANT_ID) {
            if (!currentTenant.equals(requested)) {
                throw new org.springframework.security.access.AccessDeniedException("不能管理其他租户角色");
            }
            return currentTenant;
        }
        if (requested == null || requested < 0) {
            throw new BaseException("租户id不能为空且不能为负数");
        }
        return requested;
    }

    private Integer currentTenant() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser user)) {
            throw new org.springframework.security.access.AccessDeniedException("登录状态无效");
        }
        return user.getUser().getTenantId();
    }
}
