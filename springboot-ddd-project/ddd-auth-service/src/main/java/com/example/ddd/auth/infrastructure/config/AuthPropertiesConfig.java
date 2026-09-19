package com.example.ddd.auth.infrastructure.config;

import com.example.ddd.auth.infrastructure.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link JwtProperties} 的属性绑定。
 *
 * <p><b>为什么单独一个配置类？</b>
 * {@code @ConfigurationProperties} 只是<b>标记</b>属性类，Spring Boot 需要通过
 * {@code @EnableConfigurationProperties} 或 {@code @ConfigurationPropertiesScan} 才能真正创建 Bean。
 * 集中到一个配置类里方便查找与扩展（例如以后再加 {@code LoginProperties}）。</p>
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class})
public class AuthPropertiesConfig {
}
