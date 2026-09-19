/**
 * 基础设施层 (Infrastructure)。
 *
 * <p><b>职责：</b>为领域层与应用层提供技术实现，是"细节"的居所。
 * 依赖方向：infrastructure -&gt; domain（实现 domain 定义的 Repository / Port 接口，即依赖倒置 DIP）。</p>
 *
 * <p><b>组成：</b>
 * <ul>
 *   <li>{@code persistence.po}：JPA {@code @Entity}，仅用于数据库映射，含 {@code @Version} 乐观锁。</li>
 *   <li>{@code persistence.dao}：Spring Data JPA 接口。</li>
 *   <li>{@code persistence.converter}：PO ⇄ Domain 双向转换。</li>
 *   <li>{@code persistence.repository}：domain Repository 接口的实现。</li>
 *   <li>{@code mq}：RocketMQ 领域事件发布器（实现 application 的 DomainEventPublisher 端口）。</li>
 *   <li>{@code config}：领域服务装配、事务模板、全局异常处理。</li>
 * </ul>
 *
 * <p><b>约束：</b>领域层禁止出现本层的任何类型（PO / DAO / Spring 注解）。</p>
 */
package com.example.ddd.inventory.infrastructure;
