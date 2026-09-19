package com.example.ddd.user.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 请求 DTO：创建用户（管理员使用；普通注册走 auth-service）。
 */
public record CreateUserRequest(
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String mobile,
        @Email String email,
        Integer gender
) {
}
