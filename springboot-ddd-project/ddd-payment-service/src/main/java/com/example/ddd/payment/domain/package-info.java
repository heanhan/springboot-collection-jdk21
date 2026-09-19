/**
 * 支付领域层：支付单与退款单各自守护金额、状态及重复回调不变式。
 * 支付与订单拆分以隔离资金状态和交易履约状态；通过领域事件协作，不跨库事务。
 * 本层不依赖 Spring/JPA；渠道策略为纯接口，所有现有渠道均为 Mock。
 */
package com.example.ddd.payment.domain;
