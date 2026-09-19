/**
 * 用户接口层 (Interfaces / Facade)。
 *
 * <p><b>依赖方向：</b>interfaces -&gt; application -&gt; domain &lt;- infrastructure。
 * 本层是最外侧，负责协议适配（HTTP/JSON），只做 DTO ⇄ Command/Domain 的转换与参数校验，
 * <b>不含业务逻辑</b>。</p>
 *
 * <p><b>组成：</b>
 * <ul>
 *   <li>{@code rest}：面向前端/运营的 {@code @RestController}。</li>
 *   <li>{@code facade}：实现 {@code ddd-api-contract} 中 Feign 契约的服务端点（供其他服务调用）。</li>
 *   <li>{@code dto}：请求/响应 record。</li>
 *   <li>{@code assembler}：DTO ⇄ 应用层对象/领域对象/契约 DTO 的转换。</li>
 * </ul>
 */
package com.example.ddd.inventory.interfaces;
