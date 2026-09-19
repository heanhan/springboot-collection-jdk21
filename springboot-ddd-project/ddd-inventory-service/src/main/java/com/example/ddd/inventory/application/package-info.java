/**
 * 应用层 (Application)。
 *
 * <p><b>职责：</b>编排领域对象完成用例，是"业务流程的指挥者"而非"业务规则的制定者"。
 * 负责事务边界、幂等控制、乐观锁重试、跨聚合协调（调用领域服务）、事务提交后发布领域事件。</p>
 *
 * <p><b>依赖方向：</b>application -&gt; domain。应用层<b>不写</b>库存增减规则（那属于 {@code Stock} 聚合根），
 * 也不感知 JPA / MQ / Web 等基础设施细节（通过 {@code port} 抽象 + infrastructure 实现）。</p>
 *
 * <p><b>组成：</b>
 * <ul>
 *   <li>{@code service}：应用服务（一个用例一个方法）。</li>
 *   <li>{@code command}：不可变的用例输入 record + 应用层结果对象。</li>
 *   <li>{@code port}：出站端口接口（如 {@code DomainEventPublisher}），实现在 infrastructure。</li>
 * </ul>
 */
package com.example.ddd.inventory.application;
