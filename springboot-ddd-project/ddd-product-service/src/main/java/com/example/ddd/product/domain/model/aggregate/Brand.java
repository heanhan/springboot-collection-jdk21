package com.example.ddd.product.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.util.Objects;

/**
 * 聚合根：Brand 品牌。
 *
 * <p>Brand 是独立聚合，被 Spu 通过 brandId 引用。</p>
 *
 * <p><b>不变式：</b>name 全局唯一（数据库唯一索引 + Repository 校验）。</p>
 */
public class Brand extends BaseAggregateRoot {

    private final String brandId;
    private String name;
    private String logo;
    private String story;
    private String firstLetter;
    private int sort;
    private boolean enabled;

    public static Brand create(String brandId, String name, String logo, String story, int sort) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "品牌名不能为空");
        }
        Brand b = new Brand(brandId);
        b.name = name;
        b.logo = logo;
        b.story = story;
        b.firstLetter = extractFirstLetter(name);
        b.sort = sort;
        b.enabled = true;
        return b;
    }

    public static Brand reconstitute(String brandId, String name, String logo, String story,
                                     String firstLetter, int sort, boolean enabled) {
        Brand b = new Brand(brandId);
        b.name = name;
        b.logo = logo;
        b.story = story;
        b.firstLetter = firstLetter;
        b.sort = sort;
        b.enabled = enabled;
        return b;
    }

    private Brand(String brandId) {
        this.brandId = Objects.requireNonNull(brandId);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "品牌名不能为空");
        }
        this.name = newName;
        this.firstLetter = extractFirstLetter(newName);
    }

    public void updateInfo(String logo, String story, Integer sort) {
        if (logo != null) this.logo = logo;
        if (story != null) this.story = story;
        if (sort != null) this.sort = sort;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    private static String extractFirstLetter(String name) {
        if (name == null || name.isEmpty()) return null;
        char c = Character.toUpperCase(name.charAt(0));
        return Character.isLetter(c) ? String.valueOf(c) : null;
    }

    @Override
    public String aggregateId() {
        return brandId;
    }

    public String getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public String getLogo() {
        return logo;
    }

    public String getStory() {
        return story;
    }

    public String getFirstLetter() {
        return firstLetter;
    }

    public int getSort() {
        return sort;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
