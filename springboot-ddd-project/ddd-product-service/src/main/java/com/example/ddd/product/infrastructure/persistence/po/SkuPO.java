package com.example.ddd.product.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * PO：t_sku 表映射。
 */
@Entity
@Table(name = "t_sku")
public class SkuPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "sku_id", length = 32, nullable = false)
    private String skuId;

    @Column(name = "spu_id", length = 32, nullable = false)
    private String spuId;

    @Column(name = "spu_name", length = 128, nullable = false)
    private String spuName;

    @Column(name = "sku_name", length = 255, nullable = false)
    private String skuName;

    @Column(name = "spec_json", length = 512)
    private String specJson;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "market_price", precision = 12, scale = 2)
    private BigDecimal marketPrice;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "sku_code", length = 64)
    private String skuCode;

    @Column(name = "barcode", length = 64)
    private String barcode;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "weight_gram")
    private Integer weightGram;

    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getSpuId() { return spuId; }
    public void setSpuId(String spuId) { this.spuId = spuId; }
    public String getSpuName() { return spuName; }
    public void setSpuName(String spuName) { this.spuName = spuName; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getSpecJson() { return specJson; }
    public void setSpecJson(String specJson) { this.specJson = specJson; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMarketPrice() { return marketPrice; }
    public void setMarketPrice(BigDecimal marketPrice) { this.marketPrice = marketPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getWeightGram() { return weightGram; }
    public void setWeightGram(Integer weightGram) { this.weightGram = weightGram; }
}
