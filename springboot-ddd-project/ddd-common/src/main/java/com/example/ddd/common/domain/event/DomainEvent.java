package com.example.ddd.common.domain.event;

import java.time.Instant;

/**
 * 领域事件 (Domain Event) 顶层抽象。
 *
 * <p><b>什么是领域事件？</b>
 * 领域事件表示"领域中已经发生的、对业务有意义的事情"，使用过去时命名，
 * 例如 {@code OrderPaid}、{@code StockDeducted}、{@code UserRegistered}。
 * 它是限界上下文之间<b>异步协作</b>的核心手段：一个上下文发布事件，
 * 另一个上下文订阅事件并作出反应，从而实现"高内聚、低耦合"。</p>
 *
 * <p><b>为什么用 sealed interface？</b>
 * JDK 17+ 的 sealed 让我们可以精确列出所有实现类，配合 pattern matching switch
 * 可以获得编译期穷尽性检查。业务上，事件类型必须是<b>明确、有限、可枚举</b>的，
 * 不应该允许任何人随便扩展出一个"未知事件"。</p>
 *
 * <p><b>不可变性：</b>
 * 事件一旦发生就不可修改，所有字段都是 final 且通过 record / getter 暴露。</p>
 *
 * @author ddd-learning
 */
public sealed interface DomainEvent
        permits com.example.ddd.common.domain.event.AbstractDomainEvent {

    /**
     * 事件全局唯一 ID (雪花或 UUID)，用于消费者做<b>幂等</b>判断。
     */
    String eventId();

    /**
     * 事件发生时间 (UTC 时间戳)。
     */
    Instant occurredOn();

    /**
     * 触发该事件的聚合根 ID，便于溯源。
     */
    String aggregateId();

    /**
     * 事件版本号，用于事件模型的向后兼容演进 (Schema Evolution)。
     */
    int version();

    /**
     * 事件的类型标识 (通常等于类的简单名)，MQ 中作为 Tag 使用。
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
