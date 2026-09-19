package com.example.ddd.order.application.port;

import com.example.ddd.common.domain.event.DomainEvent;

/**
 * 端口 (Port)：领域事件发布器。
 *
 * <p><b>出站端口：</b>应用层只依赖此抽象，RocketMQ 实现位于 infrastructure。
 * 实现方应保证在数据库事务<b>提交后</b>再投递（通过事务同步器），避免"事务回滚但消息已发"。</p>
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
