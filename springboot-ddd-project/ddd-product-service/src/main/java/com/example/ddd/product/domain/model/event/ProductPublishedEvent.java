package com.example.ddd.product.domain.model.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：商品已上架。
 *
 * <p><b>发布者：</b>product-service（Spu 聚合）。</p>
 * <p><b>订阅者：</b>搜索索引服务、缓存刷新服务、推荐系统。</p>
 */
public class ProductPublishedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String spuId;
    private String name;
    private String categoryId;
    private String brandId;

    public ProductPublishedEvent() {
        super();
    }

    public ProductPublishedEvent(String spuId, String name, String categoryId, String brandId) {
        super(spuId);
        this.spuId = spuId;
        this.name = name;
        this.categoryId = categoryId;
        this.brandId = brandId;
    }

    public String getSpuId() {
        return spuId;
    }

    public String getName() {
        return name;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }
}
