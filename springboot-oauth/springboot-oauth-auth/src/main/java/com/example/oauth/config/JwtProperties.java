package com.example.oauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 对应 application.yml 中的 jwt.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥（HS256 遗留字段，改用 RS256 后不再用于签名，保留仅为兼容配置绑定） */
    private String secret = "ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODNiNGM5YjQ4ZGY3N2Y2NjM3NmQyMzZlYzM5";

    /** 密钥 ID（用于 JWK 的 kid，资源服务器按 kid 匹配公钥） */
    private String keyId = "oauth-auth-rsa-key";

    /** RSA 非对称密钥配置（RS256 签名/验签） */
    private Rsa rsa = new Rsa();

    /** Access Token 过期时间（秒），默认 30 分钟 */
    private Long accessExpire = 1800L;

    /** Refresh Token 过期时间（秒），默认 7 天 */
    private Long refreshExpire = 604800L;

    /** Token 前缀 */
    private String tokenPrefix = "Bearer ";

    /** 请求头名称 */
    private String header = "Authorization";

    /**
     * RSA 密钥配置
     * <p>
     * private-key：Base64(PKCS#8) 私钥；public-key：Base64(X.509) 公钥。
     * 二者均为空时，启动会自动生成一对 RSA 密钥（仅供开发/测试，重启后失效）。
     */
    @Data
    public static class Rsa {
        private String privateKey;
        private String publicKey;
    }
}
