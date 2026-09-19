package com.example.ddd.auth.interfaces.dto;

import com.example.ddd.auth.application.dto.TokenPairDTO;

/**
 * 响应 DTO：登录 / 刷新 Token 成功。
 *
 * <p><b>为什么不直接返回 TokenPairDTO？</b>
 * 应用层 DTO 属于内部模型，接口层需要一份"面向前端"的稳定契约。
 * 二者字段可以完全一致，但通过独立类型让<b>接口版本演进</b>不影响应用层。</p>
 */
public record LoginResponse(String accessToken,
                            String refreshToken,
                            String tokenType,
                            long expiresIn,
                            String userId,
                            String username) {

    public static LoginResponse from(TokenPairDTO dto) {
        return new LoginResponse(dto.accessToken(), dto.refreshToken(),
                dto.tokenType(), dto.expiresIn(), dto.userId(), dto.username());
    }
}
