package com.example.active7.controller;

import com.example.active7.model.SysPermission;
import com.example.active7.service.rbac.RbacService;
import com.example.common.result.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rbac")
public class SysRbacController {

    private final RbacService rbacService;

    public SysRbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    public ResultBody<List<Map<String, Object>>> users() {
        return ResultBody.success(rbacService.listUsers());
    }

    @GetMapping("/roles")
    public ResultBody<List<Map<String, Object>>> roles() {
        return ResultBody.success(rbacService.listRoles());
    }

    @GetMapping("/permissions")
    public ResultBody<List<SysPermission>> permissions() {
        return ResultBody.success(rbacService.listPermissions());
    }

    @PutMapping("/users/{userId}/roles")
    public ResultBody<Void> replaceUserRoles(@PathVariable String userId,
                                              @RequestBody List<String> roleCodes) {
        rbacService.replaceUserRoles(userId, roleCodes);
        return ResultBody.success();
    }

    @PutMapping("/roles/{roleCode}/permissions")
    public ResultBody<Void> replaceRolePermissions(@PathVariable String roleCode,
                                                    @RequestBody List<String> permissionCodes) {
        rbacService.replaceRolePermissions(roleCode, permissionCodes);
        return ResultBody.success();
    }
}
