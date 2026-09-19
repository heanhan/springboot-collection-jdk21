package com.example.ddd.product.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：商品已下架。
 *
 * <p><b>订阅者：</b>购物车服务（把该 SPU 的商品置为"失效"）、搜索索引服务（删除）。</p>
 */
public class ProductOffShelfEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String spuId;
    private String reason;

    public ProductOffShelfEvent() {
        super();
    }

    public ProductOffShelfEvent(String spuId, String reason) {
        super(spuId);
        this.spuId = spuId;
        this.reason = reason;
    }

    public String getSpuId() {
        return spuId;
    }

    public String getReason() {
        return reason;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
