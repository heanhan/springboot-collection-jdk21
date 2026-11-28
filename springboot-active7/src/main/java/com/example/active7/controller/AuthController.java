package com.example.active7.controller;

import com.example.active7.security.Active7JwtProperties;
import com.example.active7.security.DemoUserDetails;
import com.example.active7.service.AuthService;
import com.example.active7.service.dto.LoginRequest;
import com.example.active7.service.dto.LoginResponse;
import com.example.common.exceptins.BizException;
import com.example.common.result.ResultBody;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Active7JwtProperties properties;

    public AuthController(AuthService authService, Active7JwtProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResultBody<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.login(request);
        setRefreshCookie(response, tokens.getRefreshToken(), (int) properties.getRefreshTokenSeconds());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("accessToken", tokens.getResponse().getAccessToken());
        result.put("tokenType", tokens.getResponse().getTokenType());
        result.put("expiresIn", tokens.getResponse().getExpiresIn());
        result.put("roles", tokens.getResponse().getRoles());
        result.put("permissions", tokens.getResponse().getPermissions());
        result.put("user", currentUser(tokens.getResponse()));
        return ResultBody.success(result);
    }

    @PostMapping("/refresh")
    public ResultBody<Map<String, Object>> refresh(
            @RequestHeader(value = "X-Refresh-Request", required = false) String marker,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!"true".equals(marker)) {
            throw new BizException(403, "刷新请求缺少 X-Refresh-Request 头");
        }
        AuthService.IssuedTokens tokens = authService.refresh(refreshToken(request));
        setRefreshCookie(response, tokens.getRefreshToken(), (int) properties.getRefreshTokenSeconds());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("accessToken", tokens.getResponse().getAccessToken());
        result.put("tokenType", tokens.getResponse().getTokenType());
        result.put("expiresIn", tokens.getResponse().getExpiresIn());
        result.put("roles", tokens.getResponse().getRoles());
        result.put("permissions", tokens.getResponse().getPermissions());
        result.put("user", currentUser(tokens.getResponse()));
        return ResultBody.success(result);
    }

    @PostMapping("/logout")
    public ResultBody<Void> logout(@AuthenticationPrincipal DemoUserDetails user,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        authService.logout(refreshToken(request));
        clearRefreshCookie(response);
        return ResultBody.success();
    }

    @GetMapping("/me")
    public ResultBody<Map<String, Object>> me(@AuthenticationPrincipal DemoUserDetails user) {
        Map<String, Object> result = currentUser(user);
        result.put("roles", roles(user));
        result.put("permissions", permissions(user));
        return ResultBody.success(result);
    }

    private Map<String, Object> currentUser(LoginResponse response) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userId", response.getUserId());
        result.put("username", response.getUsername());
        result.put("displayName", response.getDisplayName());
        result.put("department", response.getDepartment());
        return result;
    }

    private Map<String, Object> currentUser(DemoUserDetails user) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("displayName", user.getDisplayName());
        result.put("department", user.getDepartment());
        return result;
    }

    private List<String> roles(DemoUserDetails user) {
        List<String> roles = new ArrayList<String>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().startsWith("ROLE_")) {
                roles.add(authority.getAuthority());
            }
        }
        return roles;
    }

    private List<String> permissions(DemoUserDetails user) {
        List<String> permissions = new ArrayList<String>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (!authority.getAuthority().startsWith("ROLE_")) {
                permissions.add(authority.getAuthority());
            }
        }
        return permissions;
    }

    private String refreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.getRefreshCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String token, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .path(properties.getRefreshCookiePath())
                .maxAge(Duration.ofSeconds(maxAge))
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .path(properties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
