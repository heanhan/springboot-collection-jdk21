package com.example.ddd.auth.interfaces.rest;

import com.example.ddd.auth.application.command.ChangePasswordCommand;
import com.example.ddd.auth.application.command.LoginCommand;
import com.example.ddd.auth.application.command.RegisterCommand;
import com.example.ddd.auth.application.dto.TokenPairDTO;
import com.example.ddd.auth.application.service.AuthApplicationService;
import com.example.ddd.auth.infrastructure.security.LoginUserPrincipal;
import com.example.ddd.auth.interfaces.dto.ChangePasswordRequest;
import com.example.ddd.auth.interfaces.dto.LoginRequest;
import com.example.ddd.auth.interfaces.dto.LoginResponse;
import com.example.ddd.auth.interfaces.dto.RefreshRequest;
import com.example.ddd.auth.interfaces.dto.RegisterRequest;
import com.example.ddd.auth.interfaces.dto.UserInfoResponse;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 控制器：认证接口（面向前端）。
 *
 * <p><b>DDD 分层：</b>本类位于<b>用户接口层</b>，只做协议适配，<b>不含业务逻辑</b>。
 * 所有编排都在 {@link AuthApplicationService}。</p>
 *
 * <p><b>接口列表：</b>
 * <ul>
 *   <li>{@code POST /auth/register}   注册</li>
 *   <li>{@code POST /auth/login}      登录，返回双 Token</li>
 *   <li>{@code POST /auth/refresh}    刷新 Token（Rotation）</li>
 *   <li>{@code POST /auth/logout}     注销（Token 拉黑）</li>
 *   <li>{@code POST /auth/password/change} 修改密码</li>
 *   <li>{@code GET  /auth/userinfo}   获取当前登录用户信息</li>
 * </ul>
 *
 * <p><b>白名单：</b>register/login/refresh 已在 {@code SecurityConfig} 放行，无需 Token。
 * 其余接口必须携带 {@code Authorization: Bearer xxx}。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
    }

    // ============================================================
    // 注册
    // ============================================================

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest req) {
        String userId = authService.register(new RegisterCommand(
                req.username(), req.password(), req.mobile(), req.email(), req.nickname()));
        return Result.ok(userId, "注册成功");
    }

    // ============================================================
    // 登录
    // ============================================================

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                       HttpServletRequest httpReq) {
        String ip = extractClientIp(httpReq);
        TokenPairDTO dto = authService.login(new LoginCommand(req.username(), req.password(), req.device(), ip));
        return Result.ok(LoginResponse.from(dto));
    }

    // ============================================================
    // 刷新 Token
    // ============================================================

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        TokenPairDTO dto = authService.refresh(req.refreshToken());
        return Result.ok(LoginResponse.from(dto));
    }

    // ============================================================
    // 注销
    // ============================================================

    /**
     * 注销当前登录。
     *
     * <p>AccessToken 立即加入 Redis 黑名单（TTL = 剩余有效期），
     * 该用户的所有 RefreshToken 全部作废。</p>
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                               @RequestBody(required = false) RefreshRequest req) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.ok();
        }
        String accessToken = authHeader.substring(7);
        authService.logout(accessToken, req == null ? null : req.refreshToken());
        return Result.ok();
    }

    // ============================================================
    // 修改密码
    // ============================================================

    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        LoginUserPrincipal principal = currentPrincipal();
        authService.changePassword(new ChangePasswordCommand(
                principal.userId(), req.oldPassword(), req.newPassword()));
        return Result.ok(null, "密码修改成功，请重新登录");
    }

    // ============================================================
    // 获取当前用户信息
    // ============================================================

    @GetMapping("/userinfo")
    public Result<UserInfoResponse> userInfo() {
        LoginUserPrincipal principal = currentPrincipal();
        // expiresAt 从 TokenProvider 无法直接得到，这里返回 0 由前端从 JWT 自行解析
        // 生产可通过 Filter 把 expiresAt 塞进 Principal
        UserInfoResponse resp = new UserInfoResponse(
                principal.userId(), principal.username(),
                principal.roles(), principal.permissions(), 0L);
        return Result.ok(resp);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private LoginUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
        }
        return principal;
    }

    /**
     * 提取客户端 IP，兼容反向代理场景。
     */
    private String extractClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能是 "client, proxy1, proxy2"
            return ip.split(",")[0].trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return req.getRemoteAddr();
    }
}
