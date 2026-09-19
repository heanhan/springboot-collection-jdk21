package com.example.ddd.product.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.util.Objects;

/**
 * 聚合根：Category 商品类目（树形）。
 *
 * <p><b>树形建模：</b>
 * 使用 {@code parentId} + {@code codePath} 双字段：
 * <ul>
 *   <li>{@code parentId}：直接父节点，便于回溯。</li>
 *   <li>{@code codePath}：形如 "0/1/11/1101/" 的祖先路径，
 *       用 LIKE 'codePath%' 一次查出整棵子树，避免递归查询。</li>
 * </ul>
 *
 * <p><b>不变式：</b>
 * <ol>
 *   <li>name 非空。</li>
 *   <li>level 与 parentId 一致（level = parent.level + 1，根节点 level = 1）。</li>
 *   <li>层级最多 4 级（业务约束，可调整）。</li>
 * </ol>
 */
public class Category extends BaseAggregateRoot {

    public static final int MAX_LEVEL = 4;
    public static final String ROOT_ID = "0";

    private final String categoryId;
    private String parentId;
    private String codePath;
    private String name;
    private int level;
    private int sort;
    private String icon;
    private boolean enabled;

    public static Category create(String categoryId, String parentId, String parentCodePath,
                                  int parentLevel, String name, int sort, String icon) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "类目名称不能为空");
        }
        int level = ROOT_ID.equals(parentId) ? 1 : parentLevel + 1;
        if (level > MAX_LEVEL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "类目层级最多 " + MAX_LEVEL + " 级");
        }
        Category c = new Category(categoryId);
        c.parentId = parentId == null ? ROOT_ID : parentId;
        c.codePath = (parentCodePath == null ? "0/" : parentCodePath) + categoryId + "/";
        c.name = name;
        c.level = level;
        c.sort = sort;
        c.icon = icon;
        c.enabled = true;
        return c;
    }

    public static Category reconstitute(String categoryId, String parentId, String codePath,
                                        String name, int level, int sort, String icon, boolean enabled) {
        Category c = new Category(categoryId);
        c.parentId = parentId;
        c.codePath = codePath;
        c.name = name;
        c.level = level;
        c.sort = sort;
        c.icon = icon;
        c.enabled = enabled;
        return c;
    }

    private Category(String categoryId) {
        this.categoryId = Objects.requireNonNull(categoryId);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "类目名称不能为空");
        }
        this.name = newName;
    }

    public void updateSort(int sort) {
        this.sort = sort;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    @Override
    public String aggregateId() {
        return categoryId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getCodePath() {
        return codePath;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getSort() {
        return sort;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
