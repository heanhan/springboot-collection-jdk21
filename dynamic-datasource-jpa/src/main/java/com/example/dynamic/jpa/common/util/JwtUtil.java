package com.example.dynamic.jpa.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this(secret, DEFAULT_ACCESS_TTL_MINUTES);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtUtil(@Value("${security.jwt.secret}") String secret,
                   @Value("${app.token.access-ttl-minutes:30}") long accessTtlMinutes) {
        Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.secret = secret;
        this.accessTtlMillis = accessTtlMinutes * 60 * 1000L;
    }

    /**
     * 签发 access token（默认有效期），带随机 jti
     */
    public String issueToken(String username, Object extendInfo) {
        return createAccessToken(username, extendInfo, java.util.UUID.randomUUID().toString());
    }

    /**
     * 签发带指定 jti 的 access token，jti 用于登出黑名单
     */
    public String createAccessToken(String username, Object extendInfo, String jti) {
        Map<String, Object> map = new HashMap<>();
        map.put(EXTEND_INFO, extendInfo);
        return Jwts.builder()
                .claims(map)
                .id(jti)
                .issuer(ISS)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtlMillis))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTtlMillis() {
        return accessTtlMillis;
    }

    public Claims parseToken(String token) {
        return getTokenBody(token, secret);
    }

    /**
     * 过期时间 10 小时
     */
    private static final long EXPIRE_TIME = 10 * 60 * 60 * 1000L;

    public static final String TOKEN_HEADER = "Authorization";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String EMPTY_STRING = "";

    private static final long DEFAULT_ACCESS_TTL_MINUTES = 30L;

    /**
     * token秘钥
     */
    private final String secret;

    /**
     * access token 有效期（毫秒）
     */
    private final long accessTtlMillis;

    /**
     * 附带额外信息
     */
    public static final String EXTEND_INFO = "extendInfo";

    /**
     * JWT签发者
     */
    private static final String ISS = "greenbon";


    /**
     * 生成签名
     *
     * @param username   用户名
     * @param secret     加盐
     * @param extendInfo 额外参数
     * @return java.lang.String
     */
    public static String createToken(String username, String secret, Object extendInfo) {
        return createToken(username, secret, extendInfo, EXPIRE_TIME);
    }

    /**
     * 生成签名
     *
     * @param username   用户名
     * @param secret     加盐
     * @param extendInfo 额外参数
     * @param extendInfo 过期时间
     * @return java.lang.String
     */
    public static String createToken(String username, String secret, Object extendInfo, Long expireTime) {
        Map<String, Object> map = new HashMap<>();
        map.put(EXTEND_INFO, extendInfo);
        return Jwts.builder()
                .claims(map)
                .issuer(ISS)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成签名
     *
     * @param username 用户名
     * @param secret   秘钥
     * @return java.lang.String
     */
    public static String createToken(String username, String secret) {
        return createToken(username, secret, null);
    }

    /**
     * 解析jwt
     */
    public static Claims getTokenBody(String token, String secret) {
//        return Jwts.parser()
//                .setSigningKey(secret)
//                .parseClaimsJws(token)
//                .getBody();
        var signed = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer(ISS)
                .build()
                .parseSignedClaims(token);
        if (!Jwts.SIG.HS256.getId().equals(signed.getHeader().getAlgorithm())) {
            throw new io.jsonwebtoken.JwtException("不支持的签名算法");
        }
        return signed.getPayload();
    }
}
