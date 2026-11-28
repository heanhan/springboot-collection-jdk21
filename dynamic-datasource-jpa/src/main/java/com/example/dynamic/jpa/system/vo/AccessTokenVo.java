package com.example.dynamic.jpa.system.vo;

/**
 * 浏览器登录响应，refresh token 仅通过 HttpOnly Cookie 下发。
 *
 * @param accessToken 短时 JWT
 * @param tokenType   token 类型
 * @param expiresIn   accessToken 有效期（秒）
 * @author zhaojh
 */
public record AccessTokenVo(String accessToken, String tokenType, long expiresIn) {

    public static AccessTokenVo from(TokenPairVo tokenPair) {
        return new AccessTokenVo(tokenPair.accessToken(), tokenPair.tokenType(), tokenPair.expiresIn());
    }
}
