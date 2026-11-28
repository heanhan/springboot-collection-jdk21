package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import com.example.dynamic.jpa.system.vo.AddUserReqVo;
import com.example.dynamic.jpa.system.vo.ChangePasswordReq;
import com.example.dynamic.jpa.system.vo.ResetPasswordReq;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private TenantService tenantService;

    @GetMapping("/me")
    public ResultBody me() {
        JwtUser principal = principal();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", principal.getUser());
        result.put("tenant", principal.getUser().getTenantId() == 0 ? null
                : tenantService.getTenantById(principal.getUser().getTenantId()));
        result.put("roles", userService.listUserRoles(principal.getUser().getId()).stream()
                .map(role -> role.getRoleCode()).toList());
        result.put("permissions", principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(code -> !code.startsWith("ROLE_")).sorted().toList());
        return ResultBody.success(result);
    }

    @GetMapping("/permissions")
    public ResultBody permissions() {
        return ResultBody.success(principal().getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(code -> !code.startsWith("ROLE_")).sorted().toList());
    }

    @GetMapping("/menus")
    public ResultBody menus() {
        JwtUser principal = principal();
        return ResultBody.success(userService.listUserMenusForTree(principal.getUser().getId(), 1));
    }

    @PostMapping("/password/change")
    public ResultBody changePassword(@Valid @RequestBody ChangePasswordReq request) {
        JwtUser principal = principal();
        userService.editUserPassword(principal.getUser().getId(), request.getOldPassword(), request.getNewPassword());
        return ResultBody.success("密码修改成功，请重新登录");
    }

    @GetMapping("/tenants/{tenantId}/users")
    public ResultBody listUsers(@PathVariable Integer tenantId) {
        assertScope(tenantId);
        return ResultBody.success(userService.listUsers(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/users")
    public ResultBody addUser(@PathVariable Integer tenantId, @Valid @RequestBody AddUserReqVo request) {
        assertScope(tenantId);
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRoleId(request.getRoleId());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setType(request.getType());
        return ResultBody.success(userService.addUser(user));
    }

    @PutMapping("/tenants/{tenantId}/users/{userId}")
    public ResultBody editUser(@PathVariable Integer tenantId, @PathVariable Integer userId,
                               @Valid @RequestBody User request) {
        assertScope(tenantId);
        User existing = requireUser(tenantId, userId);
        request.setId(userId);
        return ResultBody.success(userService.editUser(request));
    }

    @PostMapping("/tenants/{tenantId}/users/{userId}/disable")
    public ResultBody disableUser(@PathVariable Integer tenantId, @PathVariable Integer userId) {
        assertScope(tenantId);
        User existing = requireUser(tenantId, userId);
        if (existing.getId().equals(principal().getUser().getId())) {
            throw new BaseException("不能停用当前登录账号");
        }
        existing.setStatus(2);
        userService.editUser(existing);
        return ResultBody.success("用户已停用");
    }

    @PostMapping("/tenants/{tenantId}/users/{userId}/reset-password")
    public ResultBody resetPassword(@PathVariable Integer tenantId, @PathVariable Integer userId,
                                    @Valid @RequestBody ResetPasswordReq request) {
        assertScope(tenantId);
        requireUser(tenantId, userId);
        userService.resetPassword(userId, request.getNewPassword());
        return ResultBody.success("密码重置成功");
    }

    @PutMapping("/tenants/{tenantId}/users/{userId}/roles")
    public ResultBody assignRoles(@PathVariable Integer tenantId, @PathVariable Integer userId,
                                  @RequestBody List<Integer> roleIds) {
        assertScope(tenantId);
        requireUser(tenantId, userId);
        userService.assignRoles(userId, roleIds);
        return ResultBody.success("角色分配成功");
    }

    private User requireUser(Integer tenantId, Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new BaseException("用户不存在");
        }
        return user;
    }

    private void assertScope(Integer tenantId) {
        if (principal().getUser().getTenantId() != SystemConstant.SYSTEM_TENANT_ID
                && !principal().getUser().getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("不能管理其他租户成员");
        }
    }

    private JwtUser principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser user)) {
            throw new org.springframework.security.access.AccessDeniedException("登录状态无效");
        }
        return user;
    }
}
