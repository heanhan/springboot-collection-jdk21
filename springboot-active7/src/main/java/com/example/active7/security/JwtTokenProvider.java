package com.example.active7.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final Active7JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(Active7JwtProperties properties) {
        this.properties = properties;
        if (properties.getSecret() == null || properties.getSecret().trim().isEmpty()) {
            throw new IllegalStateException("缺少 active7.security.jwt.secret 配置");
        }
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenSeconds())))
                .claim("username", username)
                .claim("type", "access")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("type", String.class))) {
            throw new JwtException("Token 类型不是 Access Token");
        }
        return claims;
    }
}
