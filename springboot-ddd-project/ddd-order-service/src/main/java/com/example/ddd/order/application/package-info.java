/**
 * 应用层 (Application)。
 *
 * <p><b>职责：</b>编排领域对象与外部网关完成用例，负责事务边界、幂等、事件发布时机。
 * <b>不含</b>订单业务规则（状态流转在 {@code OrderStateMachine}，不变式在 {@code Order} 聚合根）。</p>
 *
 * <p><b>组成：</b>
 * <ul>
 *   <li>{@code service.OrderApplicationService}：下单 / 支付 / 取消 / 发货 / 收货 / 超时扫描。</li>
 *   <li>{@code command}：不可变用例输入 record。</li>
 *   <li>{@code port}：出站端口——{@code DomainEventPublisher} 事件发布、
 *       {@code ProductGateway} / {@code InventoryGateway} 防腐层（ACL），实现均在 infrastructure。</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>application -&gt; domain；对 port 的依赖由 infrastructure 反向实现（DIP）。</p>
 */
package com.example.ddd.order.application;
