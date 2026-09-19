package com.example.ddd.product.infrastructure;

/**
 * 基础设施层 (Infrastructure Layer) —— product-service 的技术实现。
 *
 * <p><b>职责：</b>为 domain / application 提供技术能力：
 * <ul>
 *   <li>{@code persistence}：JPA PO / DAO / Converter / Repository 实现。</li>
 *   <li>{@code mq}：RocketMQ 生产者 / 消费者。</li>
 *   <li>{@code cache}：Redis 缓存 / 分布式锁。</li>
 *   <li>{@code rpc}：OpenFeign 客户端配置。</li>
 *   <li>{@code config}：Spring Bean 装配、异常处理、跨域等。</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>{@code infrastructure -> domain}（实现 domain 定义的接口），
 * 不允许 domain 反向依赖 infrastructure。</p>
 */
