package com.example.dynamic.jpa.system.vo;

/**
 * 双 token 响应体。
 *
 * @param accessToken  短时 JWT，用于访问受保护资源
 * @param refreshToken 长时不透明串，用于换取新的 token 对
 * @param tokenType    token 类型，固定 Bearer
 * @param expiresIn    accessToken 有效期（秒）
 * @author zhaojh
 */
public record TokenPairVo(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static TokenPairVo of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenPairVo(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
