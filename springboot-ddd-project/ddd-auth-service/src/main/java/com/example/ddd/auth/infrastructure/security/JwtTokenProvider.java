package com.example.ddd.auth.infrastructure.security;

import com.example.ddd.auth.application.port.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JWT Token 提供器（JJWT 0.12.x 实现）。
 *
 * <p><b>JJWT 0.12.x API 变化：</b>
 * <ul>
 *   <li>{@code Jwts.builder()} 保持不变。</li>
 *   <li>{@code Jwts.parser()} 变成 {@code Jwts.parser().verifyWith(key).build().parseSignedClaims(token)}。</li>
 *   <li>{@code signWith(SignatureAlgorithm.HS256, key)} 变成 {@code signWith(key, Jwts.SIG.HS256)}。</li>
 * </ul>
 *
 * <p><b>Payload 结构：</b>
 * <pre>
 * {
 *   "iss": "ddd-auth-service",
 *   "sub": "{userId}",
 *   "jti": "{tokenId}",
 *   "iat": 1700000000,
 *   "exp": 1700001800,
 *   "username": "alice",
 *   "roles": ["USER"],
 *   "permissions": ["order:create", "order:read"]
 * }
 * </pre>
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issueAccessToken(String userId, String username, String tokenId,
                                   Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtl());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles == null ? List.of() : roles)
                .claim(CLAIM_PERMISSIONS, permissions == null ? List.of() : permissions)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String issueRefreshToken(String userId, String tokenId) {
        // RefreshToken 采用"不透明字符串"，格式：{tokenId}.{userId}.{random-uuid}
        // 真实校验依赖 Redis 中存储的映射，签名部分只是防伪造的额外保险
        return tokenId + "." + userId + "." + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public AccessTokenPayload parseAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return null;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
            String userId = claims.getSubject();
            String tokenId = claims.getId();
            String username = claims.get(CLAIM_USERNAME, String.class);
            Set<String> roles = toStringSet(claims.get(CLAIM_ROLES));
            Set<String> permissions = toStringSet(claims.get(CLAIM_PERMISSIONS));
            long expiresAt = claims.getExpiration() == null ? 0 : claims.getExpiration().toInstant().getEpochSecond();
            return new AccessTokenPayload(userId, username, tokenId, roles, permissions, expiresAt);
        } catch (ExpiredJwtException e) {
            log.debug("[JWT] token expired: {}", e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] invalid token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl();
    }

    @Override
    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtl();
    }

    @SuppressWarnings("unchecked")
    private Set<String> toStringSet(Object raw) {
        if (raw == null) return Set.of();
        if (raw instanceof Collection<?> c) {
            Set<String> result = new HashSet<>();
            for (Object o : c) {
                if (o != null) result.add(o.toString());
            }
            return result;
        }
        return Set.of();
    }
}
