package com.example.ddd.product.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：t_category 表映射。
 */
@Entity
@Table(name = "t_category")
public class CategoryPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "category_id", length = 32, nullable = false)
    private String categoryId;

    @Column(name = "parent_id", length = 32, nullable = false)
    private String parentId;

    @Column(name = "code_path", length = 255, nullable = false)
    private String codePath;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "icon", length = 255)
    private String icon;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getCodePath() { return codePath; }
    public void setCodePath(String codePath) { this.codePath = codePath; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
