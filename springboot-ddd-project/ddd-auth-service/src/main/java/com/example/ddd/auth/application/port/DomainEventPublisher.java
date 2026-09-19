package com.example.ddd.auth.application.port;

import com.example.ddd.common.domain.event.DomainEvent;

/**
 * 出站端口：领域事件发布器（与 user-service 同名，各服务独立定义避免耦合）。
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
