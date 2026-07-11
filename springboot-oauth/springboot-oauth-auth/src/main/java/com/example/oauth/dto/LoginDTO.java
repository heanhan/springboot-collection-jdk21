package com.example.oauth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码标识（由 /auth/captcha 返回；开启验证码校验时必填） */
    private String captchaKey;

    /** 验证码（开启验证码校验时必填） */
    private String captchaCode;
}
