package com.example.ddd.inventory.domain;

/**
 * 领域层 —— inventory-service 的核心。
 *
 * <p><b>聚合设计说明：</b>
 * 概念上"仓库包含库存"，但把 Stock 放到 Warehouse 聚合内会让聚合变得巨大
 * （一个仓库可能有几万条 SKU 库存），且<b>并发扣减</b>时会产生大量乐观锁冲突。
 * 因此本项目采用工程实践中的常见折衷：
 * <ul>
 *   <li>{@code Warehouse}：独立聚合，管理仓库基本信息。</li>
 *   <li>{@code Stock}：独立聚合，keyed by (warehouseId, skuId)，
 *       使用 {@code @Version} 乐观锁保证扣减原子性。</li>
 *   <li>{@code StockTransaction}：追加式流水（Append-only Log），
 *       不属于任何聚合，通过 bizNo 与 Stock 关联。</li>
 * </ul>
 *
 * <p><b>热点方案：</b>
 * 预占（Lock）用乐观锁 + 重试（应用层控制）；
 * 高并发场景可加 Redis Lua 预扣减，DB 只做最终一致的落库。</p>
 */
