package com.example.oauth.controller;

import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.dto.ChangePasswordDTO;
import com.example.oauth.dto.ProfileUpdateDTO;
import com.example.oauth.entity.SysUser;
import com.example.oauth.security.SecurityUser;
import com.example.oauth.service.UserService;
import com.example.oauth.vo.UserInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户自助中心控制器
 * <p>
 * 面向已登录用户本人的资料查看/修改、密码修改，区别于管理员的 /admin/users。
 * 所有操作均针对当前登录用户（从 SecurityContext 取用户 ID），不涉及权限管控。
 */
@Slf4j
@RestController
@RequestMapping("/user/profile")
@Api(tags = "用户自助中心接口")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    // ==================== GET /user/profile ====================

    /**
     * 查看当前登录用户资料
     */
    @GetMapping
    @ApiOperation("查看个人资料")
    public UserInfoVO getProfile() {
        SysUser user = getCurrentUser();
        return userService.buildUserInfoVO(user);
    }

    // ==================== PUT /user/profile ====================

    /**
     * 修改当前登录用户资料
     */
    @PutMapping
    @ApiOperation("修改个人资料")
    public UserInfoVO updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        SecurityUser securityUser = getSecurityUser();
        SysUser updated = userService.updateProfile(securityUser.getUserId(), dto);
        return userService.buildUserInfoVO(updated);
    }

    // ==================== PUT /user/profile/password ====================

    /**
     * 修改当前登录用户密码（需校验旧密码）
     */
    @PutMapping("/password")
    @ApiOperation("修改个人密码")
    public Boolean changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        SecurityUser securityUser = getSecurityUser();
        userService.changePassword(securityUser.getUserId(), dto);
        return Boolean.TRUE;
    }

    // ==================== 私有方法 ====================

    private SysUser getCurrentUser() {
        SecurityUser securityUser = getSecurityUser();
        SysUser user = userService.findById(securityUser.getUserId());
        if (user == null) {
            throw new BizException(AuthErrorEnum.TOKEN_INVALID);
        }
        return user;
    }

    private SecurityUser getSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(AuthErrorEnum.NOT_LOGGED_IN);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser) {
            return (SecurityUser) principal;
        }
        throw new BizException(AuthErrorEnum.NOT_LOGGED_IN);
    }
}
