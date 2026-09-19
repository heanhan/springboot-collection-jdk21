package com.example.ddd.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 请求 DTO：登录。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password,

        /** 设备标识（可选，例如 "web-chrome"、"ios-app"），用于审计 */
        String device
) {
}
