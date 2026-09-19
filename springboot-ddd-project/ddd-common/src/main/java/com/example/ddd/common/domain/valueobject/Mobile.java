package com.example.ddd.common.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 值对象：手机号 Mobile。
 *
 * <p><b>为什么单独封装而不是直接用 String？</b>
 * 这是 DDD 中典型的"用类型系统表达业务约束"手法：
 * <ul>
 *   <li>把"必须是中国大陆 11 位手机号"这个不变式收敛到一处。</li>
 *   <li>业务代码看到 {@code Mobile} 类型，无需再校验，可读性和安全性都提升。</li>
 *   <li>提供 {@link #masked()} 等通用能力，避免各处重复实现。</li>
 * </ul>
 *
 * @param value 手机号字符串，构造时会校验格式
 * @author ddd-learning
 */
public record Mobile(String value) {

    /** 中国大陆手机号正则：1 开头，第二位 3-9，共 11 位数字 */
    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public Mobile {
        Objects.requireNonNull(value, "手机号不能为 null");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("非法的中国大陆手机号：" + trimmed);
        }
        value = trimmed;
    }

    /**
     * 脱敏展示：138****1234。
     */
    public String masked() {
        return value.substring(0, 3) + "****" + value.substring(7);
    }

    @Override
    public String toString() {
        return value;
    }
}
