package com.example.ddd.auth.infrastructure.security;

import com.example.ddd.auth.domain.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasher} 的 BCrypt 实现（适配器）。
 *
 * <p><b>为什么用 BCrypt？</b>
 * <ul>
 *   <li>内置随机盐，每次 hash 结果都不同，防彩虹表。</li>
 *   <li>可调工作因子 (cost)，随硬件进步增强抗暴力破解能力。</li>
 *   <li>Spring Security 官方推荐，广泛验证。</li>
 * </ul>
 *
 * <p>Cost 默认 10，本项目学习场景足够；生产可提到 12。</p>
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) return false;
        return encoder.matches(rawPassword, hashedPassword);
    }
}
