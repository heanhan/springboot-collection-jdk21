package com.example.dynamic.jpa.security.token;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.exception.JwtException;
import com.example.dynamic.jpa.security.AuthUserServiceImpl;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import com.example.dynamic.jpa.system.vo.TokenPairVo;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private TokenStore tokenStore;

    @Resource(name = "authUserServiceImpl")
    private UserDetailsService userDetailsService;

    @Value("${app.token.refresh-ttl-days:7}")
    private long refreshTtlDays;

    private final AccountStatusUserDetailsChecker statusChecker = new AccountStatusUserDetailsChecker();

    public TokenPairVo issueTokenPair(JwtUser jwtUser) {
        return issueTokenPair(jwtUser, UUID.randomUUID().toString());
    }

    public TokenPairVo issueTokenPair(JwtUser jwtUser, String familyId) {
        var user = jwtUser.getUser();
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String identity = AuthUserServiceImpl.identity(user.getTenantId(), user.getUsername());
        String accessToken = jwtUtil.createAccessToken(identity, LoginInfo.fromUser(user),
                UUID.randomUUID().toString(), tokenVersion);
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken();
        tokenStore.saveRefreshToken(String.valueOf(user.getId()), tokenId, identity, refreshToken,
                familyId, tokenVersion, Duration.ofDays(refreshTtlDays));
        return TokenPairVo.of(accessToken, refreshToken, jwtUtil.getAccessTtlMillis() / 1000);
    }

    public TokenPairVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken不能为空");
        }
        TokenStore.RefreshTokenInfo info = tokenStore.consumeRefreshToken(refreshToken)
                .orElseThrow(() -> new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken无效或已过期"));
        UserDetails details;
        try {
            details = userDetailsService.loadUserByUsername(info.username());
            statusChecker.check(details);
        } catch (org.springframework.security.core.AuthenticationException e) {
            tokenStore.deleteAllRefreshTokens(info.userId());
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken已失效，请重新登录");
        }
        if (!(details instanceof JwtUser jwtUser)
                || !java.util.Objects.equals(info.userId(), String.valueOf(jwtUser.getUser().getId()))
                || info.tokenVersion() != (jwtUser.getUser().getTokenVersion() == null ? 0 : jwtUser.getUser().getTokenVersion())) {
            tokenStore.deleteAllRefreshTokens(info.userId());
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "登录状态已失效，请重新登录");
        }
        return issueTokenPair(jwtUser, info.familyId());
    }

    public void logout(String bearerToken, JwtUser current, String refreshToken) {
        String userId = current != null && current.getUser() != null
                ? String.valueOf(current.getUser().getId()) : null;
        if (StringUtils.isBlank(userId) && StringUtils.isNotBlank(refreshToken)) {
            userId = tokenStore.findRefreshToken(refreshToken)
                    .map(TokenStore.RefreshTokenInfo::userId)
                    .orElse(null);
        }
        if (StringUtils.isNotBlank(userId)) {
            tokenStore.deleteAllRefreshTokens(userId);
        }
        Claims claims = parse(bearerToken);
        if (claims != null && StringUtils.isNotBlank(claims.getId()) && claims.getExpiration() != null) {
            long millis = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (millis > 0) {
                tokenStore.blacklistAccessToken(claims.getId(), Duration.ofMillis(millis));
            }
        }
    }

    private Claims parse(String bearerToken) {
        if (StringUtils.isBlank(bearerToken) || !bearerToken.startsWith(JwtUtil.TOKEN_PREFIX)) {
            return null;
        }
        try {
            return jwtUtil.parseToken(bearerToken.substring(JwtUtil.TOKEN_PREFIX.length()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String generateRefreshToken() {
        byte[] buf = new byte[48];
        RANDOM.nextBytes(buf);
        return ENCODER.encodeToString(buf);
    }

    public boolean isBlacklisted(String jti) {
        return StringUtils.isNotBlank(jti) && tokenStore.isAccessTokenBlacklisted(jti);
    }
}
