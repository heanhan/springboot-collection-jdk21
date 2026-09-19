/**
 * 物流基础设施层：以 PO 和 JSON 集合快照装配完整聚合，提供 Feign、Outbox 与可选 Mock 调度。
 * 默认不开启自动轨迹；技术实现依赖领域接口，不将 JPA/JSON 细节泄露给领域。
 * 依赖方向为 interfaces → application → domain ← infrastructure。
 */
package com.example.ddd.logistics.infrastructure;
