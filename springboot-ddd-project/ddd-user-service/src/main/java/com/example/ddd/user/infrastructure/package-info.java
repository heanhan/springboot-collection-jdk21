/**
 * 基础设施层 (Infrastructure Layer)。
 *
 * <p><b>职责：</b>为 domain 与 application 提供技术能力，包括：
 * <ul>
 *   <li>{@code persistence}：JPA PO、Spring Data DAO、Converter、Repository 实现</li>
 *   <li>{@code mq}：RocketMQ 事件发布 / 消费</li>
 *   <li>{@code rpc}：OpenFeign 客户端配置与降级</li>
 *   <li>{@code cache}：Redis 缓存与分布式锁</li>
 *   <li>{@code config}：Web / Jackson / JPA / Security 配置</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>
 * infrastructure -> domain（实现 domain 定义的 Repository 接口）。
 * 领域层禁止反向依赖 infrastructure（DIP 依赖倒置原则）。</p>
 *
 * <p><b>为什么 PO 与 Domain Model 分离？</b>
 * PO 承载持久化关注点（JPA 注解、审计字段、软删除），
 * Domain Model 承载业务语义（不变式、行为、事件）。
 * 二者通过 Converter 双向转换，各自演进互不干扰。</p>
 */
package com.example.ddd.user.infrastructure;
