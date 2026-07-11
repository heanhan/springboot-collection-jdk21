package com.example.oauth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Token 响应 VO（双 Token）
 */
@Data
@Builder
public class TokenVO {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌类型 */
    private String tokenType;

    /** Access Token 过期时间（秒） */
    private Long accessExpiresIn;

    /** Refresh Token 过期时间（秒） */
    private Long refreshExpiresIn;
}
