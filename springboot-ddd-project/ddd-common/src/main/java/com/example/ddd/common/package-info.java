/**
 * DDD 共享内核 (Shared Kernel) 根包。
 *
 * <p><b>什么是共享内核？</b>
 * 在 DDD 中，当多个限界上下文 (Bounded Context) 需要复用一部分模型时，
 * 会把这部分模型抽取到一个"共享内核"中，由所有相关上下文共同维护。
 * 共享内核应当保持<b>小、稳定、通用</b>，避免变成"什么都往里塞"的杂物间。</p>
 *
 * <p><b>本模块的内容：</b>
 * <ul>
 *   <li>{@code domain.event}     领域事件抽象基类与聚合根基类</li>
 *   <li>{@code domain.valueobject} 通用值对象 (Money / Address / Mobile / Email)</li>
 *   <li>{@code common.result}    统一 HTTP 响应封装 Result</li>
 *   <li>{@code common.exception} 业务异常 BusinessException 与错误码 ErrorCode</li>
 *   <li>{@code common.util}      通用工具：IdGenerator (雪花)、JsonUtils</li>
 *   <li>{@code infrastructure.persistence} JPA 通用可审计 PO 抽象</li>
 * </ul>
 *
 * <p><b>依赖约束：</b>
 * 本模块<b>不依赖 Spring 框架</b>（仅使用 jakarta.persistence 注解 API），
 * 目的是让 domain 层的代码可以被单元测试直接引用，且不会污染领域模型的纯粹性。</p>
 */
package com.example.ddd.common;
