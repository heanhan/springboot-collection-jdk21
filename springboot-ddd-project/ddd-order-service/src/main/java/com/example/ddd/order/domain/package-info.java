/**
 * 领域层 (Domain) —— 订单上下文的核心。
 *
 * <p><b>依赖规则：</b>本层是最内核，<b>禁止</b>依赖 Spring / JPA / Web / MQ 等任何基础设施，
 * 只依赖 JDK 与 {@code ddd-common} 的共享内核（Money / Address / DomainEvent / BusinessException）
 * 以及 {@code ddd-api-contract} 的发布语言（集成事件）。</p>
 *
 * <p><b>组成：</b>
 * <ul>
 *   <li>{@code model.aggregate.Order}：聚合根，守护订单不变式与状态流转，收集集成事件。</li>
 *   <li>{@code model.entity.OrderItem}：聚合内子实体，保存商品快照。</li>
 *   <li>{@code model.valueobject.OrderStatus}：状态枚举值对象。</li>
 *   <li>{@code statemachine.OrderStateMachine}：状态流转规则（switch 穷举）。</li>
 *   <li>{@code service.OrderPricingService}：跨概念定价领域服务。</li>
 *   <li>{@code repository.OrderRepository}：仓储接口（依赖倒置，实现在 infrastructure）。</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>interfaces -&gt; application -&gt; domain &lt;- infrastructure。</p>
 */
package com.example.ddd.order.domain;
