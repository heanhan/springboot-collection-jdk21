/**
 * 跨服务契约 (Published Language) 根包。
 *
 * <p><b>什么是发布语言？</b>
 * 在 DDD 战略设计中，当两个限界上下文需要交互时，双方必须就"数据格式"达成一致，
 * 这个共同的表达方式就叫"发布语言"。它可以是 XML Schema、Protobuf、JSON Schema，
 * 在本项目中，我们用 Java 接口 + record DTO 来表达。</p>
 *
 * <p><b>子包划分（按上下文）：</b>
 * <ul>
 *   <li>{@code auth}       认证服务契约：Token 解析、当前用户查询</li>
 *   <li>{@code user}       用户服务契约：用户资料查询、地址查询</li>
 *   <li>{@code product}    商品服务契约：SKU 详情、SPU 详情</li>
 *   <li>{@code inventory}  库存服务契约：预占 / 实扣 / 回滚</li>
 *   <li>{@code order}      订单服务契约：订单查询 + 订单领域事件</li>
 *   <li>{@code payment}    支付服务契约：支付单查询 + 支付领域事件</li>
 *   <li>{@code logistics}  物流服务契约：发货单查询 + 物流领域事件</li>
 *   <li>{@code cart}       购物车契约</li>
 * </ul>
 *
 * <p><b>依赖方向：</b>
 * 本模块依赖 ddd-common（值对象 / 事件基类），不依赖任何业务服务。
 * 所有业务服务都依赖本模块。</p>
 */
package com.example.ddd.contract;
