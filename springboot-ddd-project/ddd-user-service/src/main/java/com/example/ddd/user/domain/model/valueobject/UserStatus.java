package com.example.ddd.user.domain.model.valueobject;

/**
 * 值对象：用户状态。
 *
 * <p>状态转换规则：
 * <pre>
 *   ACTIVE  ──disable()──▶  DISABLED
 *   DISABLED ──enable()───▶  ACTIVE
 * </pre>
 * 状态转换只能通过 {@link com.example.ddd.user.domain.model.aggregate.User} 的方法完成，
 * 不允许外部直接赋值。</p>
 */
public enum UserStatus {

    /** 正常 */
    ACTIVE,

    /** 已停用（不能登录，历史数据保留） */
    DISABLED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
