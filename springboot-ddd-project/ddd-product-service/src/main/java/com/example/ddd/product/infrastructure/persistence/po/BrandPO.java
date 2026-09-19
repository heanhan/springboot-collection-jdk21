package com.example.ddd.product.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：t_brand 表映射。
 */
@Entity
@Table(name = "t_brand")
public class BrandPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "brand_id", length = 32, nullable = false)
    private String brandId;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "logo", length = 255)
    private String logo;

    @Column(name = "story", columnDefinition = "TEXT")
    private String story;

    @Column(name = "first_letter", length = 1)
    private String firstLetter;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    public String getBrandId() { return brandId; }
    public void setBrandId(String brandId) { this.brandId = brandId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }
    public String getFirstLetter() { return firstLetter; }
    public void setFirstLetter(String firstLetter) { this.firstLetter = firstLetter; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
