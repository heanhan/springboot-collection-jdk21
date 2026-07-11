package com.example.oauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置属性
 * <p>
 * 对应 application.yml 中的 security.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /** 白名单 URL（Ant 风格） */
    private Ignore ignore = new Ignore();

    /** 黑名单是否启用 */
    private Blacklist blacklist = new Blacklist();

    /** 图形验证码配置 */
    private Captcha captcha = new Captcha();

    @Data
    public static class Ignore {
        /** 不需要认证的 URL 列表 */
        private List<String> urls = new ArrayList<>();
    }

    @Data
    public static class Blacklist {
        /** 是否启用黑名单校验 */
        private Boolean enabled = true;
    }

    @Data
    public static class Captcha {
        /** 登录时是否开启图形验证码校验 */
        private Boolean enabled = true;
        /** 验证码有效期（秒） */
        private Long expireSeconds = 120L;
    }
}
