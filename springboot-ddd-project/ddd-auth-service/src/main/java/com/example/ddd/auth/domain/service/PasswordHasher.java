package com.example.ddd.auth.domain.service;

/**
 * 领域服务 (Domain Service) 接口：密码哈希器。
 *
 * <p><b>为什么在 domain 层定义接口？</b>
 * 密码哈希是<b>业务概念</b>（"用户凭据的安全存储"），但具体算法（BCrypt / Argon2）
 * 是<b>技术细节</b>。领域层定义能力接口，基础设施层用 Spring Security 的
 * {@code BCryptPasswordEncoder} 提供实现，符合依赖倒置。</p>
 *
 * <p><b>为什么不是聚合根的方法？</b>
 * 哈希算法是无状态的、可复用的、跨聚合的（管理员重置密码、用户改密码都要用），
 * 且需要注入外部实现（BCryptPasswordEncoder），因此定义为领域服务。</p>
 */
public interface PasswordHasher {

    /**
     * 对明文密码做单向哈希。
     *
     * @param rawPassword 明文
     * @return 哈希串（BCrypt 结果包含盐，形如 {@code $2a$10$...}）
     */
    String hash(String rawPassword);

    /**
     * 校验明文与哈希是否匹配。
     *
     * @param rawPassword     明文
     * @param hashedPassword  之前存储的哈希
     * @return true 表示匹配
     */
    boolean matches(String rawPassword, String hashedPassword);

    /**
     * 判断密码强度是否合规。
     * <p>规则：长度 8-32，至少包含字母和数字。</p>
     */
    default boolean isStrongEnough(String rawPassword) {
        if (rawPassword == null) return false;
        int len = rawPassword.length();
        if (len < 8 || len > 32) return false;
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}
