/**
 * 领域事件抽象。
 *
 * <p>包含 {@link com.example.ddd.common.domain.event.DomainEvent} 顶层接口
 * 与 {@link com.example.ddd.common.domain.event.AbstractDomainEvent} 抽象基类。</p>
 *
 * <p><b>依赖规则：</b>
 * 领域事件是<b>领域层</b>的产物，只依赖 JDK 与本包内的类型，禁止引入 Spring / JPA / MQ 客户端。</p>
 */
package com.example.ddd.common.domain.event;
