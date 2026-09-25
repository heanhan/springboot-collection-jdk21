package com.example.dynamic.jpa.security.token;

import java.time.Duration;
import java.util.Optional;

/**
 * 双 token 机制的服务端存储端口。
 *
 * <p>AccessToken 为无状态 JWT，仅在登出时把其 jti 写入黑名单；
 * RefreshToken 为不透明随机串，需服务端存储以支持刷新与轮换（rotation）。</p>
 *
 * @author zhaojh
 */
public interface TokenStore {

    /**
     * 保存 refresh token。
     *
     * @param userId   用户id
     * @param tokenId  refresh token 的唯一标识（用于精确删除）
     * @param username 用户名，刷新时据此重新加载并校验用户
     * @param token    refresh token 明文
     * @param ttl      有效期
     */
    void saveRefreshToken(String userId, String tokenId, String username, String token, Duration ttl);

    /**
     * 根据 refresh token 反查其归属信息（刷新流程使用）。
     */
    Optional<RefreshTokenInfo> findRefreshToken(String token);

    /**
     * 删除单个 refresh token（刷新轮换时作废旧 token）。
     */
    void deleteRefreshToken(String userId, String tokenId);

    /**
     * 删除某用户的全部 refresh token（登出/强制全端下线）。
     */
    void deleteAllRefreshTokens(String userId);

    /**
     * 将 access token 的 jti 加入黑名单，直至其自然过期。
     */
    void blacklistAccessToken(String jti, Duration ttl);

    /**
     * 判断 access token 的 jti 是否已被拉黑。
     */
    boolean isAccessTokenBlacklisted(String jti);

    /**
     * refresh token 元数据。
     */
    record RefreshTokenInfo(String userId, String tokenId, String username) {
    }
}
