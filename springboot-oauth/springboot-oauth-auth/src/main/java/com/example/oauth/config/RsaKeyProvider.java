package com.example.oauth.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥提供者
 * <p>
 * 统一持有 RS256 签名/验签所需的 RSA 密钥对：
 * <ul>
 *     <li>配置了 jwt.rsa.private-key / public-key 时，从配置加载；</li>
 *     <li>否则启动时自动生成一对 2048 位 RSA 密钥（仅供开发/测试，重启后失效）。</li>
 * </ul>
 * 私钥用于本中心签发 Token，公钥通过 JWK 端点对外暴露供资源服务器验签。
 */
@Slf4j
@Getter
@Component
public class RsaKeyProvider {

    private final JwtProperties jwtProperties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String keyId;

    public RsaKeyProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        this.keyId = jwtProperties.getKeyId();
        JwtProperties.Rsa rsa = jwtProperties.getRsa();
        try {
            if (rsa != null && StringUtils.hasText(rsa.getPrivateKey()) && StringUtils.hasText(rsa.getPublicKey())) {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                this.privateKey = (RSAPrivateKey) kf.generatePrivate(
                        new PKCS8EncodedKeySpec(Base64.getDecoder().decode(rsa.getPrivateKey())));
                this.publicKey = (RSAPublicKey) kf.generatePublic(
                        new X509EncodedKeySpec(Base64.getDecoder().decode(rsa.getPublicKey())));
                log.info("已从配置加载 RSA 密钥对（RS256），kid={}", keyId);
            } else {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.privateKey = (RSAPrivateKey) kp.getPrivate();
                this.publicKey = (RSAPublicKey) kp.getPublic();
                log.warn("未配置 jwt.rsa 密钥，已自动生成 RSA 密钥对（仅供开发/测试，重启后失效；生产环境请配置固定密钥）。kid={}", keyId);
                log.warn("自动生成公钥 Base64(X.509): {}", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
                log.warn("自动生成私钥 Base64(PKCS#8): {}", Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化 RSA 密钥失败: " + e.getMessage(), e);
        }
    }
}
