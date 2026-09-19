/**
 * 支付应用层：编排建单、渠道选择、回调、关单和全额退款，本地事务持有支付行锁。
 * MQ 消费通过 Inbox 成功后去重，业务及 Outbox 原子保存；订单取消与收款并发时补偿退款。
 * 仅依赖领域与输出端口，技术实现由基础设施注入。
 */
package com.example.ddd.payment.application;
