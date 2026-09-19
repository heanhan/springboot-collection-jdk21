/**
 * 用户接口层 (Interfaces / Presentation Layer)。
 *
 * <p><b>职责：</b>把外部世界（HTTP、gRPC、MQ 消息）翻译为对应用服务的调用，
 * 并把领域对象翻译为对外 DTO。</p>
 *
 * <p><b>子包说明：</b>
 * <ul>
 *   <li>{@code rest}：面向前端 / 管理端的 @RestController。</li>
 *   <li>{@code facade}：面向<b>其他服务</b>的内部接口，实现 ddd-api-contract 中的 Feign 契约。</li>
 *   <li>{@code dto}：请求 / 响应 DTO（record），带 Bean Validation 注解。</li>
 *   <li>{@code assembler}：Domain <-> DTO 装配器（避免领域模型泄露到 HTTP 层）。</li>
 * </ul>
 *
 * <p><b>为什么区分 rest 与 facade？</b>
 * rest 面向 UI，DTO 结构可以随前端需求变化；facade 面向其他服务，属于
 * <b>发布语言</b>的一部分，需要保持稳定。二者隔离可以让 UI 变更不影响服务间契约。</p>
 */
package com.example.ddd.user.interfaces;
