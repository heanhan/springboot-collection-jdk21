package com.example.dynamic.jpa.security.token;

import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.exception.JwtException;
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

/**
 * 双 token 编排服务：签发 token 对、刷新（含 rotation）、登出（黑名单 + 清空 refresh）。
 *
 * @author zhaojh
 */
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

    /**
     * 为已认证用户签发 access + refresh token 对。
     */
    public TokenPairVo issueTokenPair(JwtUser jwtUser) {
        String username = jwtUser.getUsername();
        String userId = String.valueOf(jwtUser.getUser().getId());
        String accessJti = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessToken(username, LoginInfo.fromUser(jwtUser.getUser()), accessJti);

        String tokenId = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken();
        tokenStore.saveRefreshToken(userId, tokenId, username, refreshToken, Duration.ofDays(refreshTtlDays));

        return TokenPairVo.of(accessToken, refreshToken, jwtUtil.getAccessTtlMillis() / 1000);
    }

    /**
     * 用 refresh token 换取新的 token 对。
     *
     * <p>Token Rotation：刷新成功后旧 refresh token 立即作废；
     * 同时重新加载并校验用户（租户有效性、账号状态），失效则拒绝刷新。</p>
     */
    public TokenPairVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken不能为空");
        }
        TokenStore.RefreshTokenInfo info = tokenStore.findRefreshToken(refreshToken)
                .orElseThrow(() -> new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken无效或已过期"));

        UserDetails details;
        try {
            details = userDetailsService.loadUserByUsername(info.username());
            statusChecker.check(details);
        } catch (org.springframework.security.core.AuthenticationException e) {
            // 用户被删/租户停用/账号锁定等，作废该 refresh token 并拒绝
            tokenStore.deleteRefreshToken(info.userId(), info.tokenId());
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "refreshToken已失效，请重新登录");
        }
        if (!(details instanceof JwtUser jwtUser)) {
            throw new JwtException(ExceptionCode.TOKEN_ERROR.getCode(), "用户身份异常");
        }
        // 作废旧 refresh token（rotation）
        tokenStore.deleteRefreshToken(info.userId(), info.tokenId());
        return issueTokenPair(jwtUser);
    }

    /**
     * 登出：将当前 access token 的 jti 拉黑至其自然过期，并清空该用户全部 refresh token。
     *
     * @param bearerToken 请求头中的完整 token（含 Bearer 前缀）
     * @param current     当前登录用户
     */
    public void logout(String bearerToken, JwtUser current) {
        if (current != null && current.getUser() != null) {
            tokenStore.deleteAllRefreshTokens(String.valueOf(current.getUser().getId()));
        }
        String jti = resolveJti(bearerToken);
        Duration remaining = resolveRemainingTtl(bearerToken);
        if (StringUtils.isNotBlank(jti)) {
            tokenStore.blacklistAccessToken(jti, remaining);
        }
    }

    private String resolveJti(String bearerToken) {
        Claims claims = parse(bearerToken);
        return claims == null ? null : claims.getId();
    }

    private Duration resolveRemainingTtl(String bearerToken) {
        Claims claims = parse(bearerToken);
        if (claims == null || claims.getExpiration() == null) {
            return Duration.ZERO;
        }
        long millis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return millis > 0 ? Duration.ofMillis(millis) : Duration.ZERO;
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

    /**
     * 供过滤器判断 access token 是否已被拉黑。
     */
    public boolean isBlacklisted(String jti) {
        return StringUtils.isNotBlank(jti) && tokenStore.isAccessTokenBlacklisted(jti);
    }
}
