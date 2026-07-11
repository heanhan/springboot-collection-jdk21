package com.example.oauth.service.impl;

import com.example.oauth.config.JwtProperties;
import com.example.oauth.config.RsaKeyProvider;
import com.example.oauth.security.SecurityUser;
import com.example.oauth.service.TokenService;
import com.example.oauth.vo.TokenVO;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 双 Token 服务实现
 * <p>
 * 核心机制：
 * 1. Access Token（JWT，默认 30 分钟）—— 用于接口访问
 * 2. Refresh Token（JWT，默认 7 天）—— 用于静默续签
 * 3. 所有 Token 均存储在 Redis（带 TTL）
 * 4. 支持滑动续期：Refresh Token 有效即可换取新 Access Token
 * 5. 登出时 Access Token 和 Refresh Token 均加入黑名单
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /** Redis Key 前缀 */
    private static final String ACCESS_KEY_PREFIX = "oauth:access:";
    private static final String REFRESH_KEY_PREFIX = "oauth:refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "oauth:blacklist:";
    private static final String USER_INFO_KEY_PREFIX = "oauth:user:";
    private static final String CLIENT_KEY_PREFIX = "oauth:client:";

    /** Token 类型 */
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_TYPE_CLIENT = "client";

    /** 滑动续期阈值（Refresh Token 剩余有效期不足 1 天时自动刷新） */
    private static final long REFRESH_SLIDING_THRESHOLD_MS = 24 * 60 * 60 * 1000L;

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RsaKeyProvider rsaKeyProvider;

    public TokenServiceImpl(JwtProperties jwtProperties, RedisTemplate<String, Object> redisTemplate,
                            RsaKeyProvider rsaKeyProvider) {
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        this.rsaKeyProvider = rsaKeyProvider;
    }

    // ==================== 生成 Token ====================

    @Override
    public TokenVO generateDualToken(SecurityUser securityUser) {
        String accessToken = generateToken(securityUser, TOKEN_TYPE_ACCESS, jwtProperties.getAccessExpire());
        String refreshToken = generateToken(securityUser, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpire());

        // 存储到 Redis（带 TTL）
        String accessKey = ACCESS_KEY_PREFIX + securityUser.getUserId() + ":" + getTokenId(accessToken);
        redisTemplate.opsForValue().set(accessKey, accessToken, jwtProperties.getAccessExpire(), TimeUnit.SECONDS);

        String refreshKey = REFRESH_KEY_PREFIX + securityUser.getUserId() + ":" + getTokenId(refreshToken);
        redisTemplate.opsForValue().set(refreshKey, refreshToken, jwtProperties.getRefreshExpire(), TimeUnit.SECONDS);

        // 缓存用户信息
        cacheUserInfo(securityUser);

        log.info("用户 [{}] 生成双 Token 成功", securityUser.getUsername());
        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessExpiresIn(jwtProperties.getAccessExpire())
                .refreshExpiresIn(jwtProperties.getRefreshExpire())
                .build();
    }

    // ==================== 校验 Access Token ====================

    @Override
    public TokenVO generateClientToken(String clientId, Collection<String> scopes, long expireSeconds) {
        long ttl = expireSeconds > 0 ? expireSeconds : jwtProperties.getAccessExpire();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttl * 1000);
        String jti = UUID.randomUUID().toString().replace("-", "");

        String token = Jwts.builder()
                .setId(jti)
                .setSubject(clientId)
                .claim("clientId", clientId)
                .claim("scopes", scopes)
                .claim("tokenType", TOKEN_TYPE_CLIENT)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();

        // 存储到 Redis（带 TTL），便于服务端追踪/吊销
        String key = CLIENT_KEY_PREFIX + clientId + ":" + jti;
        redisTemplate.opsForValue().set(key, token, ttl, TimeUnit.SECONDS);

        log.info("客户端 [{}] 生成令牌成功", clientId);
        return TokenVO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .accessExpiresIn(ttl)
                .build();
    }

    @Override
    public SecurityUser validateAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        // 去除 Bearer 前缀
        token = removePrefix(token);

        // 检查黑名单
        if (isBlacklisted(token)) {
            log.warn("Token 已在黑名单中");
            return null;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = claims.get("tokenType", String.class);
            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.warn("Token 类型不匹配，期望 access，实际 {}", tokenType);
                return null;
            }

            // Redis 二次校验
            Long userId = claims.get("userId", Long.class);
            String jti = claims.getId();
            String accessKey = ACCESS_KEY_PREFIX + userId + ":" + jti;
            Object stored = redisTemplate.opsForValue().get(accessKey);
            if (stored == null) {
                log.warn("Token 在 Redis 中不存在，可能已过期或被踢出");
                return null;
            }

            return buildSecurityUserFromClaims(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Access Token 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Access Token 解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 刷新 Token（滑动续期） ====================

    @Override
    public TokenVO refreshAccessToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }

        refreshToken = removePrefix(refreshToken);

        // 检查黑名单
        if (isBlacklisted(refreshToken)) {
            log.warn("Refresh Token 已在黑名单中");
            return null;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String tokenType = claims.get("tokenType", String.class);
            if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
                log.warn("Token 类型不匹配，期望 refresh，实际 {}", tokenType);
                return null;
            }

            // Redis 二次校验
            Long userId = claims.get("userId", Long.class);
            String jti = claims.getId();
            String refreshKey = REFRESH_KEY_PREFIX + userId + ":" + jti;
            Object stored = redisTemplate.opsForValue().get(refreshKey);
            if (stored == null) {
                log.warn("Refresh Token 在 Redis 中不存在");
                return null;
            }

            // 构建用户信息
            SecurityUser securityUser = buildSecurityUserFromClaims(claims);

            // 将旧 Refresh Token 加入黑名单
            blacklistToken(refreshToken, jwtProperties.getRefreshExpire());

            // 删除 Redis 中的旧 Refresh Token
            redisTemplate.delete(refreshKey);

            // 生成新的双 Token（滑动续期：同时刷新 Refresh Token）
            TokenVO newTokens = generateDualToken(securityUser);
            log.info("用户 [{}] 刷新 Token 成功", securityUser.getUsername());
            return newTokens;

        } catch (ExpiredJwtException e) {
            log.warn("Refresh Token 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Refresh Token 解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 黑名单 ====================

    @Override
    public void blacklistTokens(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            accessToken = removePrefix(accessToken);
            Long remaining = getRemainingExpiration(accessToken);
            if (remaining != null && remaining > 0) {
                blacklistToken(accessToken, remaining);
            }
            // 从 Redis 中删除
            removeFromRedis(accessToken, TOKEN_TYPE_ACCESS);
        }

        if (StringUtils.hasText(refreshToken)) {
            refreshToken = removePrefix(refreshToken);
            Long remaining = getRemainingExpiration(refreshToken);
            if (remaining != null && remaining > 0) {
                blacklistToken(refreshToken, remaining);
            }
            // 从 Redis 中删除
            removeFromRedis(refreshToken, TOKEN_TYPE_REFRESH);
        }

        log.info("Token 已加入黑名单");
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        token = removePrefix(token);
        String blacklistKey = BLACKLIST_KEY_PREFIX + token.hashCode();
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    // ==================== 解析 Token ====================

    @Override
    public Map<String, Object> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        token = removePrefix(token);
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 用户信息缓存 ====================

    @Override
    public void cacheUserInfo(SecurityUser securityUser) {
        String key = USER_INFO_KEY_PREFIX + securityUser.getUserId();
        redisTemplate.opsForValue().set(key, securityUser, jwtProperties.getAccessExpire(), TimeUnit.SECONDS);
    }

    @Override
    public SecurityUser getCachedUserInfo(Long userId) {
        String key = USER_INFO_KEY_PREFIX + userId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof SecurityUser) {
            SecurityUser user = (SecurityUser) obj;
            // authorities 带 @JsonIgnore 未持久化，反序列化后需从 roles/permissions 重建
            user.rebuildAuthorities();
            return user;
        }
        return null;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 JWT Token
     */
    private String generateToken(SecurityUser user, String tokenType, long expireSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString().replace("-", ""))
                .setSubject(String.valueOf(user.getUserId()))
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .claim("roles", user.getRoles())
                .claim("permissions", user.getPermissions())
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 从 Claims 构建 SecurityUser
     */
    @SuppressWarnings("unchecked")
    private SecurityUser buildSecurityUserFromClaims(Claims claims) {
        SecurityUser user = new SecurityUser();
        user.setUserId(claims.get("userId", Long.class));
        user.setUsername(claims.get("username", String.class));
        user.setNickname(claims.get("nickname", String.class));

        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection) {
            user.setRoles(new HashSet<>((Collection<String>) rolesObj));
        } else {
            user.setRoles(new HashSet<>());
        }

        Object permsObj = claims.get("permissions");
        if (permsObj instanceof Collection) {
            user.setPermissions(new HashSet<>((Collection<String>) permsObj));
        } else {
            user.setPermissions(new HashSet<>());
        }

        // 根据 roles/permissions 构建 authorities
        user.rebuildAuthorities();
        user.setStatus(1);
        return user;
    }

    /**
     * 获取 Token 的 JTI（唯一 ID）
     */
    private String getTokenId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(removePrefix(token))
                    .getBody();
            return claims.getId();
        } catch (JwtException e) {
            return "";
        }
    }

    /**
     * 获取 Token 剩余有效期（秒）
     */
    private Long getRemainingExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            long remaining = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            return remaining > 0 ? remaining : 0L;
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 将 Token 加入黑名单
     */
    private void blacklistToken(String token, long ttlSeconds) {
        String blacklistKey = BLACKLIST_KEY_PREFIX + token.hashCode();
        redisTemplate.opsForValue().set(blacklistKey, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 从 Redis 中删除 Token
     */
    private void removeFromRedis(String token, String tokenType) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Long userId = claims.get("userId", Long.class);
            String jti = claims.getId();
            String key = (TOKEN_TYPE_ACCESS.equals(tokenType) ? ACCESS_KEY_PREFIX : REFRESH_KEY_PREFIX) + userId + ":" + jti;
            redisTemplate.delete(key);
        } catch (JwtException e) {
            log.warn("从 Redis 删除 Token 失败: {}", e.getMessage());
        }
    }

    /**
     * 去除 Bearer 前缀
     */
    private String removePrefix(String token) {
        if (token != null && token.startsWith(jwtProperties.getTokenPrefix())) {
            return token.substring(jwtProperties.getTokenPrefix().length());
        }
        return token;
    }
}
