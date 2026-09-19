/**
 * 物流应用层：消费 OrderPaid，检查订单及退款状态、调用库存实扣，再保存发货单和 Outbox。
 * 签收与轨迹推进持有发货单行锁；远程幂等实扣可重试，本地不跨上下文直接访问数据库。
 */
package com.example.ddd.logistics.application;
