package com.example.ddd.product.domain.model.valueobject;

/**
 * 值对象：SKU 状态。
 *
 * <p>SKU 的状态比 SPU 简单，只有"在售"和"下架"两种；用 enum 足够。</p>
 */
public enum SkuStatus {
    /**
     * 在售，可以被下单
     */
    ON_SALE,
    /**
     * 已下架，不可下单，但历史订单不受影响
     */
    OFF_SHELF;

    public boolean purchasable() {
        return this == ON_SALE;
    }
}
