package com.example.ddd.product.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * PO：t_spu 表映射。
 *
 * <p><b>与领域模型 Spu 的区别：</b>
 * PO 是"数据形状"，只有字段与 getter/setter，供 JPA 使用；
 * Spu 是"业务模型"，包含行为方法（publish/offShelf/addSku）与不变式守护。</p>
 */
@Entity
@Table(name = "t_spu")
public class SpuPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "spu_id", length = 32, nullable = false)
    private String spuId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "brand_id", length = 32, nullable = false)
    private String brandId;

    @Column(name = "category_id", length = 32, nullable = false)
    private String categoryId;

    @Column(name = "main_image", length = 255)
    private String mainImage;

    @Column(name = "album_json", columnDefinition = "TEXT")
    private String albumJson;

    @Column(name = "detail", columnDefinition = "MEDIUMTEXT")
    private String detail;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount = 0L;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    /** 下架原因；由于 SpuPO 已经很多列，用瞬态字段承载 */
    @jakarta.persistence.Transient
    private String offShelfReason;

    public String getSpuId() { return spuId; }
    public void setSpuId(String spuId) { this.spuId = spuId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrandId() { return brandId; }
    public void setBrandId(String brandId) { this.brandId = brandId; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public String getAlbumJson() { return albumJson; }
    public void setAlbumJson(String albumJson) { this.albumJson = albumJson; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getSalesCount() { return salesCount; }
    public void setSalesCount(Long salesCount) { this.salesCount = salesCount; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public String getOffShelfReason() { return offShelfReason; }
    public void setOffShelfReason(String offShelfReason) { this.offShelfReason = offShelfReason; }
}
