package com.example.ddd.auth.domain.model.valueobject;

/**
 * 值对象：凭据状态。
 *
 * <p><b>状态转换：</b>
 * <pre>
 *   ACTIVE  --连续失败 5 次-->  LOCKED (临时，锁定 10 分钟后自动回 ACTIVE)
 *   ACTIVE  --管理员禁用-->    DISABLED (永久，需人工启用)
 *   LOCKED  --锁定期满-->      ACTIVE
 * </pre>
 */
public enum CredentialStatus {

    /** 正常 */
    ACTIVE,

    /** 因登录失败次数过多被临时锁定 */
    LOCKED,

    /** 被管理员永久停用 */
    DISABLED;

    public boolean canLogin() {
        return this == ACTIVE;
    }
}
