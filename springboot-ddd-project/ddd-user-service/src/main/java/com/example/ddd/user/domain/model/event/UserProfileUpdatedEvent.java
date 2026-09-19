package com.example.ddd.user.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：用户资料已更新。
 */
public class UserProfileUpdatedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    public UserProfileUpdatedEvent() {
        super();
    }

    public UserProfileUpdatedEvent(String userId) {
        super(userId);
    }
}
