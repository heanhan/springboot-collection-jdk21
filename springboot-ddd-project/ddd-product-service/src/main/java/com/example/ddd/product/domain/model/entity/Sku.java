package com.example.ddd.product.domain.model.entity;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.product.domain.model.valueobject.SkuStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 实体 (Entity)：SKU (Stock Keeping Unit)。
 *
 * <p><b>为什么是实体而不是聚合根？</b>
 * SKU 有独立标识（skuId），但生命周期<b>依附于 SPU</b>：
 * <ul>
 *   <li>SKU 不能脱离 SPU 独立存在。</li>
 *   <li>SKU 的所有变更都通过 {@link com.example.ddd.product.domain.model.aggregate.Spu} 聚合根进行。</li>
 *   <li>跨聚合查询 SKU 通过 Repository 提供的 {@code findSkuById} 方法（读模型）。</li>
 * </ul>
 *
 * <p><b>不变式：</b>
 * <ol>
 *   <li>price &gt; 0 且精度为 2 位小数。</li>
 *   <li>skuName 非空。</li>
 * </ol>
 *
 * <p><b>与 Spu 的一致性规则：</b>
 * Spu 聚合内至少要有 1 个 SKU 才能上架；Spu 下架时，其所有 SKU 也自动不可购买。</p>
 */
public class Sku {

    private final String skuId;
    private final String spuId;
    private String skuName;
    private String specJson;
    private String image;
    private BigDecimal price;
    private BigDecimal marketPrice;
    private String skuCode;
    private String barcode;
    private SkuStatus status;
    private Integer weightGram;

    public Sku(String skuId, String spuId, String skuName, String specJson, String image,
               BigDecimal price, BigDecimal marketPrice, String skuCode, String barcode,
               SkuStatus status, Integer weightGram) {
        this.skuId = Objects.requireNonNull(skuId, "skuId 不能为 null");
        this.spuId = Objects.requireNonNull(spuId, "spuId 不能为 null");
        this.skuName = skuName;
        this.specJson = specJson;
        this.image = image;
        this.price = normalizePrice(price);
        this.marketPrice = marketPrice == null ? null : normalizePrice(marketPrice);
        this.skuCode = skuCode;
        this.barcode = barcode;
        this.status = status == null ? SkuStatus.ON_SALE : status;
        this.weightGram = weightGram;
    }

    // ============================================================
    // 业务行为
    // ============================================================

    /**
     * 修改价格（会发出 SkuPriceChanged 事件，供缓存/搜索更新）。
     *
     * <p><b>为什么价格修改要走聚合根？</b>
     * 因为价格修改可能触发一系列业务规则（例如"促销价不能高于日常价"），
     * 这些规则在聚合根级别更完整。</p>
     */
    public void changePrice(BigDecimal newPrice) {
        BigDecimal normalized = normalizePrice(newPrice);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 价格必须大于 0");
        }
        this.price = normalized;
    }

    public void onShelf() {
        this.status = SkuStatus.ON_SALE;
    }

    public void offShelf() {
        this.status = SkuStatus.OFF_SHELF;
    }

    public void updateInfo(String skuName, String specJson, String image,
                           BigDecimal marketPrice, String skuCode, String barcode, Integer weightGram) {
        if (skuName != null && !skuName.isBlank()) this.skuName = skuName;
        if (specJson != null) this.specJson = specJson;
        if (image != null) this.image = image;
        if (marketPrice != null) this.marketPrice = normalizePrice(marketPrice);
        if (skuCode != null) this.skuCode = skuCode;
        if (barcode != null) this.barcode = barcode;
        if (weightGram != null) this.weightGram = weightGram;
    }

    private BigDecimal normalizePrice(BigDecimal p) {
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 价格不能为 null");
        return p.setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // Getters
    // ============================================================

    public String getSkuId() {
        return skuId;
    }

    public String getSpuId() {
        return spuId;
    }

    public String getSkuName() {
        return skuName;
    }

    public String getSpecJson() {
        return specJson;
    }

    public String getImage() {
        return image;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getBarcode() {
        return barcode;
    }

    public SkuStatus getStatus() {
        return status;
    }

    public Integer getWeightGram() {
        return weightGram;
    }
}
