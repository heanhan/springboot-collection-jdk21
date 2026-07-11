package com.example.oauth.service;

import com.example.oauth.security.SecurityUser;
import com.example.oauth.vo.TokenVO;

import java.util.Collection;
import java.util.Map;

/**
 * 双 Token 服务接口
 * <p>
 * 管理 Access Token 和 Refresh Token 的生成、校验、续签、黑名单
 */
public interface TokenService {

    /**
     * 生成双 Token（Access + Refresh）
     *
     * @param securityUser 用户信息
     * @return TokenVO 包含 access token 和 refresh token
     */
    TokenVO generateDualToken(SecurityUser securityUser);

    /**
     * 生成客户端令牌（client_credentials 模式，M2M 机器令牌）
     * <p>
     * 令牌使用与用户令牌相同的 RS256 私钥签名，下游资源服务器可通过 JWK 端点离线验签。
     *
     * @param clientId      客户端标识
     * @param scopes        授权范围
     * @param expireSeconds 过期时间（秒），&lt;=0 时使用全局默认值
     * @return TokenVO 仅含 access token（客户端模式无 refresh token）
     */
    TokenVO generateClientToken(String clientId, Collection<String> scopes, long expireSeconds);

    /**
     * 校验 Access Token
     *
     * @param token JWT 字符串
     * @return 解析出的用户信息，校验失败返回 null
     */
    SecurityUser validateAccessToken(String token);

    /**
     * 校验 Refresh Token 并续签（滑动续期）
     *
     * @param refreshToken 刷新令牌
     * @return 新的双 Token
     */
    TokenVO refreshAccessToken(String refreshToken);

    /**
     * 将 Token 加入黑名单（登出时调用）
     *
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     */
    void blacklistTokens(String accessToken, String refreshToken);

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token JWT 字符串
     * @return true-在黑名单中，false-不在
     */
    boolean isBlacklisted(String token);

    /**
     * 从 Token 中解析 Claims（不做黑名单校验）
     *
     * @param token JWT 字符串
     * @return Claims Map
     */
    Map<String, Object> parseToken(String token);

    /**
     * 缓存用户信息到 Redis
     *
     * @param securityUser 用户信息
     */
    void cacheUserInfo(SecurityUser securityUser);

    /**
     * 从 Redis 获取缓存的用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    SecurityUser getCachedUserInfo(Long userId);
}
