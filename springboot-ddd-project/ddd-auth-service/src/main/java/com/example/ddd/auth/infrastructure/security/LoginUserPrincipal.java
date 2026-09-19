package com.example.ddd.auth.infrastructure.security;

import java.io.Serializable;
import java.util.Set;

/**
 * Spring Security 中的登录用户主体 (Principal)。
 *
 * <p><b>为什么不实现 {@code UserDetails}？</b>
 * {@code UserDetails} 需要暴露 password / isEnabled 等方法，
 * 而 JWT 场景下我们不需要密码校验（Token 已经代表认证通过），
 * 因此定义一个更纯粹的 Principal 对象。</p>
 *
 * <p>通过 {@code SecurityContextHolder.getContext().getAuthentication().getPrincipal()}
 * 可以在任何地方拿到当前登录用户。</p>
 *
 * @param userId      用户 ID
 * @param username    登录名
 * @param tokenId     当前 Token 的 jti
 * @param roles       角色码集合
 * @param permissions 权限码集合
 */
public record LoginUserPrincipal(String userId,
                                 String username,
                                 String tokenId,
                                 Set<String> roles,
                                 Set<String> permissions) implements Serializable {
}
