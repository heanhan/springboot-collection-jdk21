package com.example.ddd.common.domain.model;

import com.example.ddd.common.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根 (Aggregate Root) 通用基类。
 *
 * <p><b>什么是聚合根？</b>
 * 聚合是一组"必须一起保持一致"的领域对象的集合，聚合根是这个集合的入口，
 * 外部对象只能通过聚合根引用聚合内部的实体和值对象。聚合根负责：
 * <ol>
 *   <li><b>身份标识</b>：全局唯一 ID，生命周期内不变。</li>
 *   <li><b>不变式守护</b>：所有业务规则通过聚合根的方法暴露，禁止外部直接改内部字段。</li>
 *   <li><b>一致性边界</b>：一个事务只修改一个聚合实例，跨聚合通过领域事件达成最终一致。</li>
 *   <li><b>事件收集</b>：业务动作产生的领域事件先"暂存"在聚合根内部，
 *       由应用服务在事务提交后统一发布。</li>
 * </ol>
 *
 * <p><b>为什么事件在聚合根内暂存？</b>
 * 领域层不允许直接依赖 MQ / Spring 事件机制，否则领域模型会被基础设施污染。
 * 因此采用"收集 - 提交"模式：领域行为 -> registerEvent -> 应用服务在 @Transactional
 * 提交后从聚合根拉取事件并发送。这样领域层保持了纯粹的 POJO。</p>
 *
 * @author ddd-learning
 */
public abstract class BaseAggregateRoot {

    /**
     * 待发布的领域事件列表。
     * <p>使用 {@code transient} 语义：不会被 JPA 持久化，也不会被 JSON 序列化到 MQ payload 之外。</p>
     */
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 注册一个领域事件到聚合根。
     *
     * <p>典型用法：在聚合根的业务方法末尾调用，例如：
     * <pre>{@code
     * public void pay(Money amount) {
     *     // ... 校验 + 状态变更
     *     this.status = OrderStatus.PAID;
     *     registerEvent(new OrderPaidEvent(this.orderId, amount));
     * }
     * }</pre>
     *
     * @param event 领域事件，不允许为 null
     */
    protected void registerEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("领域事件不能为 null");
        }
        this.domainEvents.add(event);
    }

    /**
     * 获取当前聚合根收集到的所有待发布事件（只读视图）。
     * <p>应用服务在事务提交后调用此方法拿到事件并发送 MQ。</p>
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清空事件列表。
     * <p>应用服务发布完事件后<b>必须</b>调用，否则同一个聚合实例下次操作会重复发布。</p>
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * 获取聚合根 ID 的字符串表示。
     * <p>子类必须重写，返回自己的业务 ID（例如 OrderId 的字符串）。</p>
     */
    public abstract String aggregateId();
}
