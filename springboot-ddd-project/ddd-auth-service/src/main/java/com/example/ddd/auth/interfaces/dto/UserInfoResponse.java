package com.example.ddd.auth.interfaces.dto;

import java.util.Set;

/**
 * 响应 DTO：当前登录用户信息。
 *
 * <p><b>与契约模块 CurrentUserDTO 的关系：</b>
 * 契约模块的 {@code CurrentUserDTO} 面向<b>服务间</b>通信（Feign 返回），
 * 本类面向<b>前端</b>，可以按需裁剪或扩展字段（例如加头像、会员等级等）。</p>
 */
public record UserInfoResponse(String userId,
                               String username,
                               Set<String> roles,
                               Set<String> permissions,
                               long expiresAt) {
}
