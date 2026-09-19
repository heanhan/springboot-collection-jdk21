/**
 * 支付基础设施层：JPA PO/Converter 实现仓储，唯一订单/退款业务键兜底重复请求。
 * 提供 Feign 订单查询、Mock 渠道策略和 Outbox 发布适配，不在 PO 内编写业务行为。
 * 依赖方向为 interfaces → application → domain ← infrastructure。
 */
package com.example.ddd.payment.infrastructure;
