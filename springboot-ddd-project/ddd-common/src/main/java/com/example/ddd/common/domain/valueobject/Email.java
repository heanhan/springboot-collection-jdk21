package com.example.ddd.common.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 值对象：邮箱 Email。
 *
 * <p>同 {@link Mobile}，把邮箱格式校验收敛到值对象内部。</p>
 *
 * @param value 邮箱字符串
 * @author ddd-learning
 */
public record Email(String value) {

    /** 简化版邮箱正则，够用即可 */
    private static final Pattern PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        Objects.requireNonNull(value, "邮箱不能为 null");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("非法的邮箱格式：" + trimmed);
        }
        value = trimmed.toLowerCase();
    }

    /** 域名部分（例如 @gmail.com） */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * 脱敏展示：ab***@xx.com
     */
    public String masked() {
        int at = value.indexOf('@');
        if (at <= 2) {
            return "***" + value.substring(at);
        }
        return value.substring(0, 2) + "***" + value.substring(at);
    }

    @Override
    public String toString() {
        return value;
    }
}
