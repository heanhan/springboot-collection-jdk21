/**
 * 订单基础设施层：实现仓储与远程网关端口，隔离 JPA、Feign、Outbox 和定时调度。
 * PO 与领域模型显式转换；写用例用行锁串行化，领域事件与业务数据在同一事务保存。
 * 依赖方向为 interfaces → application → domain ← infrastructure。
 */
package com.example.ddd.order.infrastructure;
