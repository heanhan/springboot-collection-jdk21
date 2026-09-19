package com.example.ddd.auth.domain.model.aggregate;

import com.example.ddd.auth.domain.model.event.PasswordChangedEvent;
import com.example.ddd.auth.domain.model.valueobject.CredentialStatus;
import com.example.ddd.auth.domain.service.PasswordHasher;
import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 聚合根 (Aggregate Root)：UserCredential 认证凭据。
 *
 * <p><b>聚合边界：</b>
 * UserCredential 单独成聚合，不持有 LoginSession 集合，因为：
 * <ul>
 *   <li>LoginSession 生命周期短（30 分钟～7 天），量可能大。</li>
 *   <li>Session 的读写模式（Redis 优先）与 Credential（MySQL）不同。</li>
 *   <li>二者的一致性通过 userId 关联即可，不需要聚合级事务。</li>
 * </ul>
 *
 * <p><b>不变式：</b>
 * <ol>
 *   <li>username 全局唯一（由 Repository + 数据库唯一索引共同保证）。</li>
 *   <li>passwordHash 不能为空，且必须是 BCrypt 格式（长度 60）。</li>
 *   <li>status = DISABLED 时禁止任何登录 / 改密操作。</li>
 *   <li>连续失败 &gt;= 5 次自动锁定 10 分钟。</li>
 * </ol>
 *
 * <p><b>生命周期：</b>
 * 注册 -> 登录（可能失败计数） -> 改密 -> 停用/删除。</p>
 */
public class UserCredential extends BaseAggregateRoot {

    /** 最大连续失败次数 */
    public static final int MAX_FAIL_COUNT = 5;

    /** 锁定时长（分钟） */
    public static final int LOCK_MINUTES = 10;

    private final String userId;
    private final String username;
    private String passwordHash;
    private String mobile;
    private String email;
    private CredentialStatus status;
    private int failCount;
    private LocalDateTime lockUntil;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordUpdateTime;

    // ============================================================
    // 工厂方法
    // ============================================================

    /**
     * 注册新凭据。
     *
     * @param userId       由应用服务预先生成（雪花 ID）
     * @param username     登录名
     * @param rawPassword  明文密码（不入库，只用于生成 hash）
     * @param mobile       手机号
     * @param email        邮箱，可为 null
     * @param hasher       密码哈希领域服务
     */
    public static UserCredential register(String userId, String username, String rawPassword,
                                          String mobile, String email, PasswordHasher hasher) {
        Objects.requireNonNull(userId, "userId 不能为 null");
        Objects.requireNonNull(username, "username 不能为 null");
        if (!hasher.isStrongEnough(rawPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "密码强度不足：长度 8-32，必须包含字母和数字");
        }
        UserCredential c = new UserCredential(userId, username, hasher.hash(rawPassword),
                mobile, email, CredentialStatus.ACTIVE);
        c.passwordUpdateTime = LocalDateTime.now();
        return c;
    }

    /**
     * 从持久化数据重建。
     */
    public static UserCredential reconstitute(String userId, String username, String passwordHash,
                                              String mobile, String email, CredentialStatus status,
                                              int failCount, LocalDateTime lockUntil,
                                              LocalDateTime lastLoginTime, String lastLoginIp,
                                              LocalDateTime passwordUpdateTime) {
        UserCredential c = new UserCredential(userId, username, passwordHash, mobile, email, status);
        c.failCount = failCount;
        c.lockUntil = lockUntil;
        c.lastLoginTime = lastLoginTime;
        c.lastLoginIp = lastLoginIp;
        c.passwordUpdateTime = passwordUpdateTime;
        return c;
    }

    private UserCredential(String userId, String username, String passwordHash,
                           String mobile, String email, CredentialStatus status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.mobile = mobile;
        this.email = email;
        this.status = status;
    }

    // ============================================================
    // 业务行为：登录
    // ============================================================

    /**
     * 校验登录前置状态：账号可用 + 未被临时锁定。
     *
     * <p><b>注意：</b>此方法不做密码校验，密码校验由应用服务调用
     * {@link PasswordHasher#matches(String, String)} 完成，
     * 因为 PasswordHasher 是领域服务，不便于聚合根直接持有。</p>
     */
    public void assertCanLogin() {
        if (this.status == CredentialStatus.DISABLED) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        // 若锁定到期，自动解锁
        if (this.status == CredentialStatus.LOCKED) {
            if (this.lockUntil != null && LocalDateTime.now().isAfter(this.lockUntil)) {
                this.status = CredentialStatus.ACTIVE;
                this.failCount = 0;
                this.lockUntil = null;
            } else {
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                        "账号锁定中，请在 " + this.lockUntil + " 后重试");
            }
        }
    }

    /**
     * 记录登录成功：清零失败计数，记录时间与 IP。
     */
    public void recordLoginSuccess(String ip) {
        this.failCount = 0;
        this.lockUntil = null;
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
    }

    /**
     * 记录登录失败：累加计数，达阈值则锁定。
     */
    public void recordLoginFailure() {
        this.failCount++;
        if (this.failCount >= MAX_FAIL_COUNT) {
            this.status = CredentialStatus.LOCKED;
            this.lockUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_TOO_FREQUENT,
                    "登录失败次数过多，账号锁定 " + LOCK_MINUTES + " 分钟");
        }
    }

    // ============================================================
    // 业务行为：密码修改
    // ============================================================

    /**
     * 修改密码。
     *
     * @param oldRawPassword 原密码（明文）
     * @param newRawPassword 新密码（明文）
     * @param hasher         哈希服务
     */
    public void changePassword(String oldRawPassword, String newRawPassword, PasswordHasher hasher) {
        if (this.status == CredentialStatus.DISABLED) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!hasher.matches(oldRawPassword, this.passwordHash)) {
            throw new BusinessException(ErrorCode.AUTH_OLD_PASSWORD_INCORRECT);
        }
        if (!hasher.isStrongEnough(newRawPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "新密码强度不足：长度 8-32，必须包含字母和数字");
        }
        this.passwordHash = hasher.hash(newRawPassword);
        this.passwordUpdateTime = LocalDateTime.now();
        // 修改密码后清零失败计数
        this.failCount = 0;
        if (this.status == CredentialStatus.LOCKED) {
            this.status = CredentialStatus.ACTIVE;
            this.lockUntil = null;
        }
        registerEvent(new PasswordChangedEvent(this.userId, this.username));
    }

    /**
     * 管理员重置密码（不需要原密码）。
     */
    public void resetPassword(String newRawPassword, PasswordHasher hasher) {
        if (!hasher.isStrongEnough(newRawPassword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码强度不足");
        }
        this.passwordHash = hasher.hash(newRawPassword);
        this.passwordUpdateTime = LocalDateTime.now();
        registerEvent(new PasswordChangedEvent(this.userId, this.username));
    }

    /**
     * 停用账号（永久）。
     */
    public void disable() {
        this.status = CredentialStatus.DISABLED;
    }

    /**
     * 启用账号。
     */
    public void enable() {
        this.status = CredentialStatus.ACTIVE;
        this.failCount = 0;
        this.lockUntil = null;
    }

    // ============================================================
    // Getters
    // ============================================================

    @Override
    public String aggregateId() {
        return this.userId;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public CredentialStatus getStatus() { return status; }
    public int getFailCount() { return failCount; }
    public LocalDateTime getLockUntil() { return lockUntil; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public String getLastLoginIp() { return lastLoginIp; }
    public LocalDateTime getPasswordUpdateTime() { return passwordUpdateTime; }
}
