package com.example.oauth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.oauth.common.enums.AuthErrorEnum;
import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.config.SecurityProperties;
import com.example.oauth.service.CaptchaService;
import com.example.oauth.vo.CaptchaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 * <p>
 * 基于 Hutool 生成线段干扰验证码，验证码答案存入 Redis 并设置有效期，校验后一次性失效。
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** Redis 中验证码 key 前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "oauth:captcha:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;

    public CaptchaServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              SecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    public CaptchaVO generate() {
        // 宽 120 高 40，4 位字符，20 条干扰线
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode();
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        long expireSeconds = securityProperties.getCaptcha().getExpireSeconds();
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaKey, code, expireSeconds, TimeUnit.SECONDS);

        return CaptchaVO.builder()
                .captchaKey(captchaKey)
                .captchaImage(lineCaptcha.getImageBase64Data())
                .expireSeconds(expireSeconds)
                .build();
    }

    @Override
    public void validate(String captchaKey, String captchaCode) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            throw new BizException(AuthErrorEnum.CAPTCHA_INVALID);
        }
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            throw new BizException(AuthErrorEnum.CAPTCHA_EXPIRED);
        }
        // 无论成功失败均删除，防止暴力尝试
        redisTemplate.delete(redisKey);
        if (!cached.toString().equalsIgnoreCase(captchaCode)) {
            throw new BizException(AuthErrorEnum.CAPTCHA_INVALID);
        }
    }
}
