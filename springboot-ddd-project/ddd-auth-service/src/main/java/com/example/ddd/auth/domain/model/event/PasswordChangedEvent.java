package com.example.ddd.auth.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：密码已修改。
 *
 * <p>发布后，其他服务（例如 user-service、notification-service）可以：
 * <ul>
 *   <li>强制所有已登录会话下线（把该用户所有 AccessToken 加入黑名单）。</li>
 *   <li>发送安全通知邮件。</li>
 * </ul>
 */
public class PasswordChangedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;

    public PasswordChangedEvent() {
        super("deserialization-placeholder");
    }

    public PasswordChangedEvent(String userId, String username) {
        super(userId);
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
}
