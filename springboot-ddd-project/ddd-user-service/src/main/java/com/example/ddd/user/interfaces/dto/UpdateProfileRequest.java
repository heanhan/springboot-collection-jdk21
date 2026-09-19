package com.example.ddd.user.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 请求 DTO：修改资料。
 */
public record UpdateProfileRequest(
        @Size(max = 64) String nickname,
        @Size(max = 255) String avatar,
        Integer gender,
        @Email String email
) {
}
