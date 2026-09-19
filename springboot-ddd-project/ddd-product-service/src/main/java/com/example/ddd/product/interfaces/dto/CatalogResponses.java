package com.example.ddd.product.interfaces.dto;

import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.model.aggregate.Category;

/**
 * 响应 DTO：类目 / 品牌。
 */
public final class CatalogResponses {

    private CatalogResponses() {
    }

    public record CategoryResponse(String categoryId,
                                   String parentId,
                                   String codePath,
                                   String name,
                                   int level,
                                   int sort,
                                   String icon,
                                   boolean enabled) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getCategoryId(), c.getParentId(), c.getCodePath(),
                    c.getName(), c.getLevel(), c.getSort(), c.getIcon(), c.isEnabled());
        }
    }

    public record BrandResponse(String brandId,
                                String name,
                                String logo,
                                String story,
                                String firstLetter,
                                int sort,
                                boolean enabled) {
        public static BrandResponse from(Brand b) {
            return new BrandResponse(b.getBrandId(), b.getName(), b.getLogo(), b.getStory(),
                    b.getFirstLetter(), b.getSort(), b.isEnabled());
        }
    }
}
