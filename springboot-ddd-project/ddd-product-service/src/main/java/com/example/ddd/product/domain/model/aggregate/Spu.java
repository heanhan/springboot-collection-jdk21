package com.example.ddd.product.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.product.domain.model.entity.Sku;
import com.example.ddd.product.domain.model.event.ProductOffShelfEvent;
import com.example.ddd.product.domain.model.event.ProductPublishedEvent;
import com.example.ddd.product.domain.model.valueobject.ProductStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 聚合根 (Aggregate Root)：Spu 商品。
 *
 * <p><b>聚合边界：</b>
 * Spu + 其下所有 Sku 构成一个聚合。Sku 是聚合内实体，不允许脱离 Spu 单独修改。
 * Brand / Category 通过 ID 引用（跨聚合引用只用 ID，不用对象），保证聚合小。</p>
 *
 * <p><b>不变式（Invariants）：</b>
 * <ol>
 *   <li>name 非空。</li>
 *   <li>上架时至少要有一个 SkuStatus = ON_SALE 的 Sku。</li>
 *   <li>Draft 状态不能被下单；OffShelf 状态不能被下单。</li>
 *   <li>删除 Sku 时，如果剩余 Sku 数量 = 0 且当前是 OnSale，则必须先下架。</li>
 * </ol>
 *
 * <p><b>生命周期：</b>
 * Draft（草稿）-> OnSale（上架）-> OffShelf（下架）-> OnSale（可再次上架）。</p>
 *
 * <p><b>为什么状态用 sealed interface？</b>
 * 见 {@link ProductStatus}。</p>
 */
public class Spu extends BaseAggregateRoot {

    private final String spuId;
    private String name;
    private String subtitle;
    private String brandId;
    private String categoryId;
    private String mainImage;
    private String albumJson;
    private String detail;
    private ProductStatus status;
    private long salesCount;
    private LocalDateTime publishTime;
    private String offShelfReason;

    private final List<Sku> skus = new ArrayList<>();

    // ============================================================
    // 工厂方法
    // ============================================================

    /**
     * 创建新 SPU（草稿态）。
     */
    public static Spu create(String spuId, String name, String subtitle,
                             String brandId, String categoryId, String mainImage,
                             String albumJson, String detail) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SPU 名称不能为空");
        }
        if (brandId == null || categoryId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "brandId / categoryId 不能为空");
        }
        Spu spu = new Spu(spuId);
        spu.name = name;
        spu.subtitle = subtitle;
        spu.brandId = brandId;
        spu.categoryId = categoryId;
        spu.mainImage = mainImage;
        spu.albumJson = albumJson;
        spu.detail = detail;
        spu.status = new ProductStatus.Draft();
        spu.salesCount = 0;
        return spu;
    }

    /**
     * 从持久化数据重建。
     */
    public static Spu reconstitute(String spuId, String name, String subtitle,
                                   String brandId, String categoryId, String mainImage,
                                   String albumJson, String detail,
                                   String statusCode, long salesCount, LocalDateTime publishTime,
                                   String offShelfReason, List<Sku> skus) {
        Spu spu = new Spu(spuId);
        spu.name = name;
        spu.subtitle = subtitle;
        spu.brandId = brandId;
        spu.categoryId = categoryId;
        spu.mainImage = mainImage;
        spu.albumJson = albumJson;
        spu.detail = detail;
        long publishMs = publishTime == null ? 0
                : publishTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        spu.status = ProductStatus.of(statusCode, publishMs, offShelfReason);
        spu.salesCount = salesCount;
        spu.publishTime = publishTime;
        spu.offShelfReason = offShelfReason;
        if (skus != null) spu.skus.addAll(skus);
        return spu;
    }

    private Spu(String spuId) {
        this.spuId = Objects.requireNonNull(spuId, "spuId 不能为 null");
    }

    // ============================================================
    // 业务行为：SKU 管理
    // ============================================================

    public Sku addSku(Sku sku) {
        if (!sku.getSpuId().equals(this.spuId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 的 spuId 与聚合根不一致");
        }
        this.skus.add(sku);
        return sku;
    }

    public Optional<Sku> findSku(String skuId) {
        return skus.stream().filter(s -> s.getSkuId().equals(skuId)).findFirst();
    }

    public void removeSku(String skuId) {
        Sku target = findSku(skuId).orElseThrow(() ->
                new BusinessException(ErrorCode.PRODUCT_SKU_NOT_FOUND));
        // 保护不变式：在售 SPU 至少要有一个 SKU
        if (this.status instanceof ProductStatus.OnSale && this.skus.size() == 1) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_ILLEGAL,
                    "在售 SPU 至少保留一个 SKU，请先下架再删除");
        }
        this.skus.remove(target);
    }

    // ============================================================
    // 业务行为：上下架
    // ============================================================

    /**
     * 上架 SPU。
     *
     * <p><b>不变式：</b>
     * <ul>
     *   <li>必须至少有一个 ON_SALE 状态的 SKU。</li>
     *   <li>已上架的不能重复上架（幂等由应用服务判断）。</li>
     * </ul>
     */
    public void publish() {
        if (this.status instanceof ProductStatus.OnSale) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_ON_SALE);
        }
        if (skus.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_ILLEGAL, "SPU 尚未添加任何 SKU，不能上架");
        }
        boolean anyPurchasable = skus.stream().anyMatch(s -> s.getStatus().purchasable());
        if (!anyPurchasable) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_ILLEGAL, "所有 SKU 都已下架，不能上架 SPU");
        }
        LocalDateTime now = LocalDateTime.now();
        this.status = new ProductStatus.OnSale(System.currentTimeMillis());
        if (this.publishTime == null) this.publishTime = now;
        registerEvent(new ProductPublishedEvent(this.spuId, this.name, this.categoryId, this.brandId));
    }

    /**
     * 下架 SPU。
     */
    public void offShelf(String reason) {
        if (this.status instanceof ProductStatus.OffShelf) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_OFF_SHELF);
        }
        if (this.status instanceof ProductStatus.Draft) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_ILLEGAL, "草稿态商品无需下架");
        }
        this.status = new ProductStatus.OffShelf(reason == null ? "" : reason);
        this.offShelfReason = reason;
        registerEvent(new ProductOffShelfEvent(this.spuId, reason));
    }

    public void updateInfo(String name, String subtitle, String mainImage,
                           String albumJson, String detail, String categoryId, String brandId) {
        if (name != null && !name.isBlank()) this.name = name;
        if (subtitle != null) this.subtitle = subtitle;
        if (mainImage != null) this.mainImage = mainImage;
        if (albumJson != null) this.albumJson = albumJson;
        if (detail != null) this.detail = detail;
        if (categoryId != null) this.categoryId = categoryId;
        if (brandId != null) this.brandId = brandId;
    }

    public void increaseSalesCount(long delta) {
        if (delta <= 0) return;
        this.salesCount += delta;
    }

    // ============================================================
    // 状态查询
    // ============================================================

    public boolean isPurchasable() {
        return this.status.purchasable()
                && skus.stream().anyMatch(s -> s.getStatus().purchasable());
    }

    @Override
    public String aggregateId() {
        return spuId;
    }

    // ============================================================
    // Getters
    // ============================================================

    public String getSpuId() {
        return spuId;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getBrandId() {
        return brandId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getMainImage() {
        return mainImage;
    }

    public String getAlbumJson() {
        return albumJson;
    }

    public String getDetail() {
        return detail;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public String getOffShelfReason() {
        return offShelfReason;
    }

    public List<Sku> getSkus() {
        return Collections.unmodifiableList(skus);
    }
}
