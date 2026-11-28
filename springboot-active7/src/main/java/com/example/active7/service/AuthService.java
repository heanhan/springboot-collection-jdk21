package com.example.active7.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.model.SysUser;
import com.example.active7.security.Active7JwtProperties;
import com.example.active7.security.DemoUserDetails;
import com.example.active7.security.DemoUserDetailsService;
import com.example.active7.security.JwtTokenProvider;
import com.example.active7.service.dto.LoginRequest;
import com.example.active7.service.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final DemoUserDetailsService userDetailsService;
    private final SysUserMapper sysUserMapper;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final Active7JwtProperties properties;

    public AuthService(AuthenticationManager authenticationManager,
                       DemoUserDetailsService userDetailsService,
                       SysUserMapper sysUserMapper,
                       RefreshTokenService refreshTokenService,
                       JwtTokenProvider jwtTokenProvider,
                       Active7JwtProperties properties) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.sysUserMapper = sysUserMapper;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.properties = properties;
    }

    public IssuedTokens login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return issue((DemoUserDetails) authentication.getPrincipal());
    }

    @Transactional(noRollbackFor = RefreshTokenService.RefreshTokenSecurityException.class)
    public IssuedTokens refresh(String rawRefreshToken) {
        String userId = refreshTokenService.rotate(rawRefreshToken);
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .last("FOR UPDATE"));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            refreshTokenService.revokeAllForUser(userId);
            throw new RefreshTokenService.RefreshTokenSecurityException("账号不存在或已禁用");
        }
        return issue((DemoUserDetails) userDetailsService.loadUserByUsername(user.getUsername()));
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private IssuedTokens issue(DemoUserDetails user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUsername());
        String refreshToken = refreshTokenService.create(user.getUserId());
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(properties.getAccessTokenSeconds());
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setDepartment(user.getDepartment());
        List<String> roles = new ArrayList<String>();
        List<String> permissions = new ArrayList<String>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().startsWith("ROLE_")) {
                roles.add(authority.getAuthority());
            } else {
                permissions.add(authority.getAuthority());
            }
        }
        response.setRoles(roles);
        response.setPermissions(permissions);
        return new IssuedTokens(response, refreshToken);
    }

    public static class IssuedTokens {

        private final LoginResponse response;
        private final String refreshToken;

        private IssuedTokens(LoginResponse response, String refreshToken) {
            this.response = response;
            this.refreshToken = refreshToken;
        }

        public LoginResponse getResponse() {
            return response;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
