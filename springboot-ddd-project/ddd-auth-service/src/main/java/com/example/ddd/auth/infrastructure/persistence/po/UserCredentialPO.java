package com.example.ddd.auth.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * PO：认证凭据表。
 */
@Entity
@Table(name = "t_user_credential")
public class UserCredentialPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "password_hash", length = 128, nullable = false)
    private String passwordHash;

    @Column(name = "mobile", length = 20, nullable = false)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    @Column(name = "pwd_update_time")
    private LocalDateTime pwdUpdateTime;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public LocalDateTime getPwdUpdateTime() { return pwdUpdateTime; }
    public void setPwdUpdateTime(LocalDateTime pwdUpdateTime) { this.pwdUpdateTime = pwdUpdateTime; }
}
