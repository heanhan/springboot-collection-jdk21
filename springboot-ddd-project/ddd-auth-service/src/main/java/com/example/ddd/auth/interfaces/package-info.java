package com.example.ddd.auth.interfaces;

/**
 * 用户接口层（User Interface Layer / Presentation Layer）。
 *
 * <p><b>职责：</b>协议适配。把 HTTP 请求翻译成对 application 层的 Command 调用，
 * 把领域对象/DTO 翻译成 HTTP 响应。</p>
 *
 * <p><b>依赖方向：</b>{@code interfaces -> application -> domain <- infrastructure}。
 * interfaces 层<b>可以</b>依赖 Spring Web、Validation、Jackson 等技术框架，
 * 但<b>禁止</b>依赖 JPA、Feign、Redis 等基础设施组件（这些属于 infrastructure）。</p>
 *
 * <p><b>子包说明：</b>
 * <ul>
 *   <li>{@code rest}：面向外部前端 / 网关的 REST 控制器。</li>
 *   <li>{@code facade}：面向内部服务的 Feign 服务端点（实现契约模块中的接口）。</li>
 *   <li>{@code dto}：请求 / 响应 DTO（record），仅在本层流转。</li>
 *   <li>{@code assembler}：DTO 与 Command / Domain 的转换工具。</li>
 * </ul>
 */
