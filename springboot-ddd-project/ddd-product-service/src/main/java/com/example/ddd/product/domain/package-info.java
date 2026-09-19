package com.example.ddd.product.domain;

/**
 * 领域层 (Domain Layer) —— product-service 的核心。
 *
 * <p><b>严格约束：</b>
 * <ul>
 *   <li>禁止依赖 Spring / JPA / Jackson 等框架。</li>
 *   <li>禁止 {@code jakarta.persistence.*} / {@code org.springframework.*} 出现在类字段或方法签名上。</li>
 *   <li>Repository 只定义接口，实现放在 infrastructure 层。</li>
 * </ul>
 *
 * <p><b>子包：</b>
 * <ul>
 *   <li>{@code model.aggregate}：聚合根（Spu / Category / Brand）。</li>
 *   <li>{@code model.entity}：聚合内实体（Sku）。</li>
 *   <li>{@code model.valueobject}：值对象（ProductStatus / SkuStatus）。</li>
 *   <li>{@code model.event}：领域事件。</li>
 *   <li>{@code repository}：仓储接口。</li>
 *   <li>{@code service}：领域服务（跨聚合规则）。</li>
 * </ul>
 */
