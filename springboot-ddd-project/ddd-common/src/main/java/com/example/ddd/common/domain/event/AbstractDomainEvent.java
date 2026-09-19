package com.example.ddd.common.domain.event;

import com.example.ddd.common.util.IdGenerator;

import java.time.Instant;

/**
 * 领域事件抽象基类。
 *
 * <p>提供事件公共字段的默认实现：eventId 使用雪花算法生成，
 * occurredOn 使用当前 UTC 时间，version 默认为 1。</p>
 *
 * <p><b>为什么允许继承 sealed 接口？</b>
 * {@link DomainEvent} 使用 sealed 限定实现范围，本类作为唯一"非 final 实现"，
 * 允许各业务模块继承它来定义自己的具体事件（例如 {@code OrderPaidEvent}）。
 * 这样既保证了顶层类型可穷尽枚举，又允许业务侧灵活扩展。</p>
 *
 * @author ddd-learning
 */
@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
public abstract non-sealed class AbstractDomainEvent implements DomainEvent {

    /** 事件全局唯一 ID，构造时自动生成，序列化后不可变。 */
    private final String eventId;

    /** 事件发生时间，构造时自动生成。 */
    private final Instant occurredOn;

    /** 触发该事件的聚合根 ID。 */
    private final String aggregateId;

    /** 事件版本号，默认 1。 */
    private final int version;

    /**
     * 构造一个领域事件。
     *
     * @param aggregateId 聚合根 ID，不允许为 null
     */
    /** Jackson 重建事件时不生成新标识，由 JSON 恢复原事件字段。 */
    protected AbstractDomainEvent() {
        this(null, null, null, 1);
    }

    protected AbstractDomainEvent(String aggregateId) {
        this(aggregateId, 1);
    }

    /**
     * 构造一个指定版本的领域事件。
     *
     * @param aggregateId 聚合根 ID
     * @param version     事件版本号 (>= 1)
     */
    protected AbstractDomainEvent(String aggregateId, int version) {
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId 不能为空");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version 必须 >= 1");
        }
        this.eventId = IdGenerator.nextIdStr();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
        this.version = version;
    }

    /**
     * 反序列化专用构造 (供 Jackson / MQ 消费者使用)。
     */
    protected AbstractDomainEvent(String eventId, Instant occurredOn, String aggregateId, int version) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.aggregateId = aggregateId;
        this.version = version;
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return aggregateId;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + eventId + '\'' +
                ", occurredOn=" + occurredOn +
                ", aggregateId='" + aggregateId + '\'' +
                ", version=" + version +
                '}';
    }
}
