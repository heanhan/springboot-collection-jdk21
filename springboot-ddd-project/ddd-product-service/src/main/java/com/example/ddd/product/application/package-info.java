package com.example.ddd.product.application;

/**
 * 应用层 (Application Layer) —— product-service 的用例编排。
 *
 * <p><b>职责：</b>
 * <ul>
 *   <li>协调领域对象完成用例（不含业务规则本身）。</li>
 *   <li>管理事务边界（{@code @Transactional}）。</li>
 *   <li>事务提交后发布领域事件。</li>
 *   <li>调用出站端口（Repository / Publisher / RPC）。</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>{@code interfaces -> application -> domain}。
 * application 可以依赖 domain，但 domain 不能反向依赖 application。</p>
 *
 * <p><b>子包：</b>
 * <ul>
 *   <li>{@code command}：写用例的输入 DTO (record)。</li>
 *   <li>{@code query}：读用例的输入 DTO。</li>
 *   <li>{@code service}：应用服务，一个用例对应一个方法。</li>
 *   <li>{@code port}：出站端口（Repository 已在 domain 层，此处放非核心端口如 MQ Publisher）。</li>
 * </ul>
 */
