package com.example.oauth.service;

import com.example.oauth.vo.CaptchaVO;

/**
 * 图形验证码服务
 */
public interface CaptchaService {

    /**
     * 生成图形验证码并写入 Redis
     *
     * @return 验证码标识 + Base64 图片
     */
    CaptchaVO generate();

    /**
     * 校验验证码（校验后立即失效，一次性使用）。
     * <p>
     * 校验失败或已过期时抛出 {@link com.example.oauth.common.exceptions.BizException}。
     *
     * @param captchaKey  验证码标识
     * @param captchaCode 用户输入的验证码
     */
    void validate(String captchaKey, String captchaCode);
}
