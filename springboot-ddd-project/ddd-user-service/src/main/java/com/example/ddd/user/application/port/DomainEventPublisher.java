package com.example.ddd.user.application.port;

import com.example.ddd.common.domain.event.DomainEvent;

/**
 * 出站端口 (Outbound Port)：领域事件发布器。
 *
 * <p><b>为什么在 application 层定义接口，在 infrastructure 层实现？</b>
 * 这是"六边形架构 (Hexagonal)" / "端口-适配器 (Ports &amp; Adapters)" 的经典应用：
 * <ul>
 *   <li>application 层只关心"我需要发布事件"这个能力（端口）。</li>
 *   <li>infrastructure 层提供具体的 MQ 实现（适配器：RocketMQ / Kafka / RabbitMQ）。</li>
 *   <li>未来切换 MQ 只需替换实现，业务代码零改动。</li>
 * </ul>
 *
 * <p><b>为什么不用 Spring 的 ApplicationEventPublisher？</b>
 * Spring 事件是进程内的，跨服务通信需要 MQ。此处保留一个抽象接口，
 * 实现类 {@code RocketMqEventPublisher} 会把事件序列化后发到 RocketMQ。</p>
 *
 * @author ddd-learning
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件。
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);
}
