package com.example.ddd.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 请求 DTO：刷新 Token。
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken
) {
}
