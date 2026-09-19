package com.example.ddd.auth.application.port;

/**
 * 出站端口：Token 签发器 (JWT Provider)。
 *
 * <p><b>为什么在 application 层定义？</b>
 * Token 签发是"能力"（业务需要），JJWT 库是"实现"（技术细节）。
 * 领域/应用层只关心"给我一个签名后的字符串"，具体算法/密钥在基础设施层配置。</p>
 */
public interface TokenProvider {

    /**
     * 签发 AccessToken。
     *
     * @param userId      用户 ID
     * @param username    登录名
     * @param tokenId     Token 唯一 ID (jti)，用于黑名单
     * @param roles       角色码集合（序列化到 payload）
     * @param permissions 权限码集合
     * @return JWT 字符串
     */
    String issueAccessToken(String userId, String username, String tokenId,
                            java.util.Set<String> roles, java.util.Set<String> permissions);

    /**
     * 签发 RefreshToken（不透明字符串，同时会存入 Redis）。
     *
     * @return 形如 {@code uuid.hmac-signature} 的字符串
     */
    String issueRefreshToken(String userId, String tokenId);

    /**
     * 解析并校验 AccessToken，返回 payload。
     *
     * @param accessToken JWT
     * @return 若签名有效且未过期返回 payload，否则返回 null
     */
    AccessTokenPayload parseAccessToken(String accessToken);

    /**
     * AccessToken 有效期（秒）。
     */
    long getAccessTokenTtlSeconds();

    /**
     * RefreshToken 有效期（秒）。
     */
    long getRefreshTokenTtlSeconds();

    /**
     * AccessToken Payload 结构。
     */
    record AccessTokenPayload(String userId, String username, String tokenId,
                              java.util.Set<String> roles, java.util.Set<String> permissions,
                              long expiresAt) {}
}
