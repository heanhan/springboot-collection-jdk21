package com.example.ddd.auth.application.port;

import java.time.Duration;
import java.util.Optional;

/**
 * 出站端口 (Outbound Port)：Token 存储。
 *
 * <p><b>抽象出这个端口的原因：</b>
 * 认证的核心业务是"签发/校验 Token"，但 Token 存储介质是技术细节（Redis / DB / 内存）。
 * 领域层不关心，应用层通过端口调用，基础设施层实现（{@code RedisTokenStore}）。</p>
 *
 * <p><b>三类 Key：</b>
 * <ul>
 *   <li>RefreshToken：{@code auth:refresh:{userId}:{tokenId}}，存 7 天。</li>
 *   <li>AccessToken 黑名单：{@code auth:blacklist:{tokenId}}，TTL = Token 剩余有效期。</li>
 *   <li>登录失败计数（可选，也可放在聚合根中）。</li>
 * </ul>
 */
public interface TokenStore {

    /**
     * 保存 RefreshToken。
     *
     * @param userId  用户 ID
     * @param tokenId Token 唯一 ID (jti)
     * @param token   完整 RefreshToken 字符串
     * @param ttl     有效期
     */
    void saveRefreshToken(String userId, String tokenId, String token, Duration ttl);

    /**
     * 根据 RefreshToken 反查 userId + tokenId（用于刷新流程）。
     *
     * @param refreshToken 完整 Token 字符串
     * @return 若存在返回 userId + tokenId 组合
     */
    Optional<RefreshTokenInfo> findRefreshToken(String refreshToken);

    /**
     * 删除 RefreshToken（登出 / 刷新时旧 Token 失效）。
     */
    void deleteRefreshToken(String userId, String tokenId);

    /**
     * 删除某个用户的所有 RefreshToken（强制下线全部设备）。
     */
    void deleteAllRefreshTokens(String userId);

    /**
     * 把 AccessToken 加入黑名单（登出时）。
     *
     * @param tokenId AccessToken 的 jti
     * @param ttl     剩余有效期
     */
    void blacklistAccessToken(String tokenId, Duration ttl);

    /**
     * 判断 AccessToken 是否已被拉黑。
     */
    boolean isAccessTokenBlacklisted(String tokenId);

    /**
     * RefreshToken 元数据。
     */
    record RefreshTokenInfo(String userId, String tokenId) {}
}
