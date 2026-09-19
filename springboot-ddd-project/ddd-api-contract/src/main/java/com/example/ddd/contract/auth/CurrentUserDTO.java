package com.example.ddd.contract.auth;

import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息 DTO。
 *
 * <p>由 auth 服务解析 JWT 后返回，其他服务据此完成鉴权与业务归属判断。</p>
 *
 * @param userId      用户 ID
 * @param username    用户名
 * @param tokenId     本次登录的 Token 唯一 ID (jti)，用于强制下线
 * @param roles       角色码集合，例如 ["USER", "ADMIN"]
 * @param permissions 权限码集合，例如 ["order:create"]
 * @param expiresAt   AccessToken 过期时间（epoch 秒）
 * @author ddd-learning
 */
public record CurrentUserDTO(String userId,
                             String username,
                             String tokenId,
                             Set<String> roles,
                             Set<String> permissions,
                             long expiresAt) implements Serializable {
}
