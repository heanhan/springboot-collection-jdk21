package com.example.ddd.inventory.application.port;

import com.example.ddd.common.domain.event.DomainEvent;

/**
 * 端口 (Port)：领域事件发布器。
 *
 * <p><b>六边形架构中的"出站端口"：</b>
 * 应用层只依赖此抽象接口，具体实现（RocketMQ）位于 infrastructure 层。
 * 这样领域/应用层无需感知 MQ 技术细节，也便于单元测试时用内存实现替换。</p>
 */
public interface DomainEventPublisher {

    /**
     * 发布一个领域事件。
     * <p>实现方应保证：在数据库事务<b>提交后</b>再真正投递，避免"事务回滚但消息已发"。</p>
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);
}
