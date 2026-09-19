package com.example.ddd.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性绑定。
 *
 * <p>对应 {@code application.yml} 中的：
 * <pre>
 * ddd:
 *   jwt:
 *     secret: xxx
 *     access-token-ttl: 1800
 *     refresh-token-ttl: 604800
 *     issuer: ddd-auth-service
 * </pre>
 */
@ConfigurationProperties(prefix = "ddd.jwt")
public class JwtProperties {

    /** HS256 对称密钥，至少 32 字节 */
    private String secret = "ddd-learning-project-jwt-secret-key-please-change-in-production-32bytes-min";

    /** AccessToken 有效期（秒） */
    private long accessTokenTtl = 1800L;

    /** RefreshToken 有效期（秒） */
    private long refreshTokenTtl = 604800L;

    /** 签发者 */
    private String issuer = "ddd-auth-service";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(long accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
    public long getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(long refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
