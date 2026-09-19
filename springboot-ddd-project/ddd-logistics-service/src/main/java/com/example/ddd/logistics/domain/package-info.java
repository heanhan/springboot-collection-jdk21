/**
 * 物流领域层：Shipment 聚合封装商品快照、追加轨迹、运输状态和幂等签收。
 * 履约与订单拆分，使承运商和轨迹模型独立演进；不感知 Spring、JPA 或 JSON。
 * 本地实体标识限定在聚合内，不向外暴露可修改集合。
 */
package com.example.ddd.logistics.domain;
