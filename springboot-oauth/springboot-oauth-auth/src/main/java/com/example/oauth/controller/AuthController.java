package com.example.oauth.controller;

import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.config.SecurityProperties;
import com.example.oauth.dto.LoginDTO;
import com.example.oauth.dto.RefreshTokenDTO;
import com.example.oauth.dto.RegisterDTO;
import com.example.oauth.entity.SysUser;
import com.example.oauth.security.SecurityUser;
import com.example.oauth.service.CaptchaService;
import com.example.oauth.service.LoginAttemptService;
import com.example.oauth.service.TokenService;
import com.example.oauth.service.UserService;
import com.example.oauth.vo.CaptchaVO;
import com.example.oauth.vo.MenuVO;
import com.example.oauth.vo.TokenVO;
import com.example.oauth.vo.UserInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 认证授权控制器
 * <p>
 * 提供登录、登出、刷新 Token、获取当前用户信息、获取菜单树等接口
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Api(tags = "认证授权接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final CaptchaService captchaService;
    private final SecurityProperties securityProperties;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          UserService userService,
                          LoginAttemptService loginAttemptService,
                          CaptchaService captchaService,
                          SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
        this.securityProperties = securityProperties;
    }

    // ==================== GET /auth/captcha ====================

    /**
     * 获取图形验证码（返回 captchaKey + Base64 图片）
     */
    @GetMapping("/captcha")
    @ApiOperation("获取图形验证码")
    public CaptchaVO captcha() {
        return captchaService.generate();
    }

    // ==================== POST /auth/login ====================

    /**
     * 用户名+密码登录（JSON 方式，返回双 Token）
     * <p>
     * 表单方式（application/x-www-form-urlencoded）的登录由 Spring Security 的
     * UsernamePasswordAuthenticationFilter 在 /auth/form-login 端点处理，LoginSuccessHandler 返回双 Token。
     * 此方法处理 JSON 方式的登录请求。
     */
    @PostMapping("/login")
    @ApiOperation("用户名密码登录（JSON）")
    public TokenVO login(@Valid @RequestBody LoginDTO loginDTO) {
        String username = loginDTO.getUsername();

        // 开启验证码时，先校验图形验证码
        if (Boolean.TRUE.equals(securityProperties.getCaptcha().getEnabled())) {
            captchaService.validate(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode());
        }

        // 检查是否被锁定
        if (loginAttemptService.isLocked(username)) {
            long remaining = loginAttemptService.getLockTimeRemaining(username);
            throw new BizException(AuthErrorEnum.USER_LOCKED.getResultCode(),
                    "账号已被锁定，请 " + remaining + " 分钟后重试");
        }

        try {
            // 使用 AuthenticationManager 认证
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, loginDTO.getPassword());
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 认证成功
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            loginAttemptService.resetAttempts(username);

            // 生成双 Token
            return tokenService.generateDualToken(securityUser);

        } catch (BadCredentialsException e) {
            // 认证失败：密码错误
            loginAttemptService.recordFailedAttempt(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            if (loginAttemptService.isLocked(username)) {
                long lockMinutes = loginAttemptService.getLockTimeRemaining(username);
                throw new BizException(AuthErrorEnum.LOGIN_FAIL_LIMIT.getResultCode(),
                        "登录失败次数过多，账号已被锁定 " + lockMinutes + " 分钟");
            }
            throw new BizException(AuthErrorEnum.TOKEN_INVALID.getResultCode(),
                    "用户名或密码错误，剩余尝试次数: " + remaining);
        } catch (LockedException e) {
            throw new BizException(AuthErrorEnum.USER_LOCKED);
        }
    }

    // ==================== POST /auth/register ====================

    /**
     * 用户自助注册（返回注册成功的用户 ID）
     */
    @PostMapping("/register")
    @ApiOperation("用户自助注册")
    public Long register(@Valid @RequestBody RegisterDTO registerDTO) {
        SysUser user = userService.register(registerDTO);
        return user.getId();
    }

    // ==================== POST /auth/refresh ====================

    /**
     * 刷新 Token（滑动续期）
     */
    @PostMapping("/refresh")
    @ApiOperation("刷新 Token")
    public TokenVO refresh(@Valid @RequestBody RefreshTokenDTO refreshDTO) {
        TokenVO tokenVO = tokenService.refreshAccessToken(refreshDTO.getRefreshToken());
        if (tokenVO == null) {
            throw new BizException(AuthErrorEnum.REFRESH_TOKEN_INVALID);
        }
        return tokenVO;
    }

    // ==================== POST /auth/logout ====================
    // 登出由 Spring Security 的 LogoutFilter + LogoutSuccessHandlerImpl 统一处理
    // （端点 /auth/logout，将 Access/Refresh Token 加入黑名单），此处不再重复定义

    // ==================== GET /auth/current ====================

    /**
     * 获取当前登录用户信息及权限
     */
    @GetMapping("/current")
    @ApiOperation("获取当前登录用户信息")
    public UserInfoVO currentUser() {
        SecurityUser securityUser = getSecurityUser();
        SysUser sysUser = userService.findById(securityUser.getUserId());
        if (sysUser == null) {
            throw new BizException(AuthErrorEnum.TOKEN_INVALID);
        }
        return userService.buildUserInfoVO(sysUser);
    }

    // ==================== GET /auth/menus ====================

    /**
     * 获取当前用户菜单树
     */
    @GetMapping("/menus")
    @ApiOperation("获取当前用户菜单树")
    public List<MenuVO> menus() {
        SecurityUser securityUser = getSecurityUser();
        return userService.getUserMenuTree(securityUser.getUserId());
    }

    // ==================== 私有方法 ====================

    /**
     * 从 SecurityContext 获取当前登录用户
     */
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
