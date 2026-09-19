package com.example.ddd.order.application.port;

import com.example.ddd.common.domain.valueobject.Money;

import java.util.Optional;

/**
 * 端口 (Port) / 防腐层 (ACL)：商品上下文网关。
 *
 * <p><b>为什么需要防腐层？</b>
 * 订单上下文不应直接依赖商品上下文的 Feign DTO（{@code SkuDTO}），否则商品契约变化会侵蚀订单领域。
 * 本接口用订单自己理解的语言（{@link SkuSnapshot} + {@link Money}）表达"我需要什么"，
 * 具体到 Feign 调用与 DTO 翻译由 infrastructure 层的适配器完成。这就是 DDD 的 Anti-Corruption Layer。</p>
 */
public interface ProductGateway {

    /**
     * 查询 SKU 快照（下单前校验 + 取价）。
     *
     * @param skuId SKU ID
     * @return 快照；不存在返回 empty
     */
    Optional<SkuSnapshot> getSku(String skuId);

    /**
     * 值对象：订单视角的 SKU 快照。
     *
     * @param skuId   SKU ID
     * @param spuId   SPU ID
     * @param spuName SPU 名称
     * @param skuName SKU 名称
     * @param image   主图
     * @param price   售价
     * @param onSale  是否在售
     */
    record SkuSnapshot(String skuId, String spuId, String spuName, String skuName,
                       String image, Money price, boolean onSale) {
    }
}
