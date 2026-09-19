/**
 * 应用层 (Application Layer)。
 *
 * <p><b>职责：</b>
 * <ul>
 *   <li>编排领域对象完成一个业务用例 (Use Case)。</li>
 *   <li>管理事务边界 (@Transactional)。</li>
 *   <li>发布领域事件到 MQ（事务提交后）。</li>
 *   <li>调用外部服务 (Feign)、缓存 (Redis)。</li>
 * </ul>
 *
 * <p><b>不允许：</b>
 * <ul>
 *   <li>编写核心业务规则（应下沉到 domain 层）。</li>
 *   <li>直接依赖 JPA / HTTP Servlet API（应通过 domain repository 接口 + interfaces 层转换）。</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>application -> domain（可以）；application -> infrastructure（禁止直接引用，通过接口反向依赖）。</p>
 */
package com.example.ddd.user.application;
