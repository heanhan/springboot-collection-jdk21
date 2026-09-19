/**
 * 基础设施层通用抽象。
 *
 * <p>{@link com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO}
 * 为所有 JPA PO 提供审计字段（createTime / updateTime / version / deleted）。</p>
 *
 * <p><b>注意：</b>共享内核中出现 JPA 注解并不违反 DDD 原则 —— 这是<b>基础设施</b>包，
 * 领域层不会引用它，只有各服务的 infrastructure/persistence/po 才继承它。</p>
 */
package com.example.ddd.common.infrastructure.persistence;
