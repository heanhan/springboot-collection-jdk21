package com.example.oauth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码标识（登录时随用户名密码一并回传） */
    private String captchaKey;

    /** 验证码图片（Base64 Data URI，可直接用于 img src） */
    private String captchaImage;

    /** 有效期（秒） */
    private Long expireSeconds;
}
