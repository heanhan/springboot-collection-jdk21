package com.example.oauth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 刷新 Token 请求 DTO
 */
@Data
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
