package com.example.ddd.user.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：用户已停用 (UserDisabled)。
 *
 * <p><b>订阅者：</b>auth-service（把该用户的所有 Token 加入黑名单，强制下线）。</p>
 */
public class UserDisabledEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String reason;

    public UserDisabledEvent() {
        super();
    }

    public UserDisabledEvent(String userId, String reason) {
        super(userId);
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
