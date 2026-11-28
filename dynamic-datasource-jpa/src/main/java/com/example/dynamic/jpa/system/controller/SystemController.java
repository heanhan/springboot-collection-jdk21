package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.security.AuthUserServiceImpl;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.security.LoginUser;
import com.example.dynamic.jpa.security.token.TokenService;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import com.example.dynamic.jpa.system.vo.AccessTokenVo;
import com.example.dynamic.jpa.system.vo.AddUserReqVo;
import com.example.dynamic.jpa.system.vo.RefreshReqVo;
import com.example.dynamic.jpa.system.vo.TokenPairVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api")
@MyDataSource(type = DataSourceType.SYSTEM)
public class SystemController {

    public static final String REFRESH_COOKIE_NAME = "dynjpa_refresh";

    @Resource
    public UserService userService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private TokenService tokenService;

    @Resource
    private TenantService tenantService;

    @Resource
    private RoleService roleService;

    @Value("${app.token.refresh-ttl-days}")
    private long refreshTtlDays;

    @Value("${app.token.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @Value("${app.token.refresh-cookie-same-site}")
    private String refreshCookieSameSite;

    @PostMapping(value = "/addTenantInfo")
    public ResultBody addTenantInfo(@jakarta.validation.Valid @RequestBody AddUserReqVo reqVo) {
        JwtUser principal = currentPrincipal();
        Integer currentTenant = principal.getUser().getTenantId();
        Integer targetTenant = reqVo.getTenantId();
        if (currentTenant == null
                || (currentTenant != SystemConstant.SYSTEM_TENANT_ID && !currentTenant.equals(targetTenant))) {
            throw new org.springframework.security.access.AccessDeniedException("不能为其他租户创建账号");
        }
        if (targetTenant > SystemConstant.SYSTEM_TENANT_ID && tenantService.getTenantById(targetTenant) == null) {
            throw new IllegalArgumentException("租户不存在");
        }
        Role role = roleService.getRoleById(reqVo.getRoleId());
        if (role == null || !java.util.Objects.equals(role.getTenantId(), targetTenant)) {
            throw new IllegalArgumentException("角色不存在或不属于当前租户");
        }
        User user = new User();
        user.setUsername(reqVo.getUsername());
        user.setNickname(StringUtils.defaultIfBlank(reqVo.getNickname(), reqVo.getUsername()));
        user.setPassword(reqVo.getPassword());
        user.setPhone(reqVo.getPhone());
        user.setEmail(reqVo.getEmail());
        user.setRoleId(role.getId());
        user.setTenantId(targetTenant);
        user.setType(reqVo.getType());
        userService.addUser(user);
        return ResultBody.success("用户注册成功！");
    }

    @PostMapping(value = "/login")
    public ResultBody login(@jakarta.validation.Valid @RequestBody LoginUser loginUser,
                            HttpServletRequest request, HttpServletResponse response) {
        Integer tenantId = loginUser.getTenantid() == null ? SystemConstant.SYSTEM_TENANT_ID : loginUser.getTenantid();
        String identity = AuthUserServiceImpl.identity(tenantId, loginUser.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(identity, loginUser.getPassword()));
        TokenPairVo tokenPair = tokenService.issueTokenPair((JwtUser) authentication.getPrincipal());
        setRefreshCookie(request, response, tokenPair.refreshToken());
        return ResultBody.success(AccessTokenVo.from(tokenPair));
    }

    @PostMapping(value = "/refresh")
    public ResultBody refresh(@RequestBody(required = false) RefreshReqVo reqVo,
                              @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshCookie,
                              HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = StringUtils.isNotBlank(refreshCookie)
                ? refreshCookie : reqVo == null ? null : reqVo.getRefreshToken();
        TokenPairVo tokenPair = tokenService.refresh(refreshToken);
        setRefreshCookie(request, response, tokenPair.refreshToken());
        return ResultBody.success(AccessTokenVo.from(tokenPair));
    }

    @PostMapping(value = "/logout")
    public ResultBody logout(@RequestHeader(value = JwtUtil.TOKEN_HEADER, required = false) String authHeader,
                             @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshCookie,
                             HttpServletRequest request, HttpServletResponse response) {
        tokenService.logout(authHeader, currentPrincipal(), refreshCookie);
        clearRefreshCookie(request, response);
        return ResultBody.success("登出成功");
    }

    private void setRefreshCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure || request.isSecure())
                .sameSite(refreshCookieSameSite)
                .path(request.getContextPath() + "/api")
                .maxAge(Duration.ofDays(refreshTtlDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure || request.isSecure())
                .sameSite(refreshCookieSameSite)
                .path(request.getContextPath() + "/api")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private JwtUser currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser principal)) {
            return null;
        }
        return principal;
    }
}
