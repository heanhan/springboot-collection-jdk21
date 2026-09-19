/**
 * 领域层 (Domain Layer) —— 认证上下文的核心业务模型。
 *
 * <p><b>本层禁止：</b>
 * <ul>
 *   <li>import 任何 {@code org.springframework.*} 类（除了 stereotype 之外的）</li>
 *   <li>import 任何 {@code jakarta.persistence.*} 类</li>
 *   <li>直接调用 Redis / MQ / HTTP</li>
 * </ul>
 * 这些技术能力应通过 domain 层定义的<b>接口</b>（如 {@code PasswordHasher}）反向依赖注入。</p>
 *
 * <p><b>本层允许：</b>
 * <ul>
 *   <li>纯 Java 语法 + JDK 21 特性（record / sealed / pattern matching）</li>
 *   <li>共享内核 ddd-common 的值对象、异常、事件基类</li>
 * </ul>
 */
package com.example.ddd.auth.domain;
