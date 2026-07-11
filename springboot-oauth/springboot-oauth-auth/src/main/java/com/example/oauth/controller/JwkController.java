package com.example.oauth.controller;

import com.example.oauth.config.RsaKeyProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWK 公钥端点
 * <p>
 * 对外暴露 RS256 验签公钥（JWK Set 格式），资源服务器可通过
 * jwk-set-uri 指向 /oauth/token_key 或 /.well-known/jwks.json 拉取公钥进行本地验签，
 * 无需与认证中心共享私钥或对称密钥。
 */
@Api(tags = "JWK 公钥端点")
@RestController
public class JwkController {

    private final RsaKeyProvider rsaKeyProvider;

    public JwkController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @ApiOperation("获取 JWK 公钥集合（供资源服务器验签）")
    @GetMapping({"/oauth/token_key", "/.well-known/jwks.json"})
    public Map<String, Object> jwks() {
        RSAPublicKey pub = rsaKeyProvider.getPublicKey();
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", rsaKeyProvider.getKeyId());
        jwk.put("n", enc.encodeToString(toUnsignedBytes(pub.getModulus())));
        jwk.put("e", enc.encodeToString(toUnsignedBytes(pub.getPublicExponent())));

        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", Collections.singletonList(jwk));
        return jwks;
    }

    /**
     * BigInteger 转无符号大端字节数组（去掉 Java 补的符号位 0x00），符合 JWK base64url 规范
     */
    private byte[] toUnsignedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }
}
