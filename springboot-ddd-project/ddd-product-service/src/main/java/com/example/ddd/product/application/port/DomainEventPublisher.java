package com.example.ddd.product.application.port;

import com.example.ddd.common.domain.event.DomainEvent;

/**
 * 出站端口：领域事件发布器。
 *
 * <p><b>为什么在 application 层定义？</b>
 * 事件发布是"业务能力"（我需要通知别人发生了什么），RocketMQ 是"技术实现"。
 * 应用层只依赖抽象，具体走 MQ / Spring Event / 日志由 infrastructure 层决定。</p>
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件。
     */
    void publish(DomainEvent event);
}
