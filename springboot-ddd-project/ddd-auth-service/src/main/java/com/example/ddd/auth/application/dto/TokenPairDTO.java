package com.example.ddd.auth.application.dto;

/**
 * 应用层 DTO：登录结果（双 Token）。
 *
 * <p><b>为什么用 record？</b>
 * 应用层向接口层返回的"数据袋"，不需要行为，record 简洁不可变。</p>
 *
 * @param accessToken   JWT 短期令牌
 * @param refreshToken  不透明长期令牌（仅存 Redis）
 * @param tokenType     固定 "Bearer"
 * @param expiresIn     AccessToken 剩余秒数
 * @param userId        用户 ID
 * @param username      登录名
 */
public record TokenPairDTO(String accessToken,
                           String refreshToken,
                           String tokenType,
                           long expiresIn,
                           String userId,
                           String username) {

    public static TokenPairDTO of(String accessToken, String refreshToken, long expiresIn,
                                  String userId, String username) {
        return new TokenPairDTO(accessToken, refreshToken, "Bearer", expiresIn, userId, username);
    }
}
