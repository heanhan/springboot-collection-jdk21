package com.example.ddd.product.domain.service;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.repository.BrandRepository;
import com.example.ddd.product.domain.repository.CategoryRepository;

/**
 * 领域服务：商品上架校验。
 *
 * <p><b>为什么这些逻辑不放到 Spu 聚合根内？</b>
 * 上架校验涉及<b>跨聚合</b>（Spu / Category / Brand）的一致性判断：
 * <ul>
 *   <li>Category 必须存在且启用。</li>
 *   <li>Brand 必须存在且启用。</li>
 *   <li>Spu 内部规则（至少一个可售 SKU）由聚合根自己保证。</li>
 * </ul>
 * 如果把这些跨聚合的查询塞进 Spu，会让 Spu 依赖 Category/Brand 的 Repository，
 * 违反"聚合只关心自己"的原则。<b>领域服务</b>是解决"跨聚合业务规则"的标准位置。</p>
 *
 * <p><b>与应用服务的区别：</b>
 * 领域服务只包含<b>纯业务规则</b>，不涉及事务/MQ/HTTP；应用服务负责编排 + 事务边界。</p>
 */
public class ProductPublishService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public ProductPublishService(CategoryRepository categoryRepository, BrandRepository brandRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    /**
     * 上架前置校验：类目/品牌必须存在且启用，SPU 内部规则由 {@link Spu#publish()} 保证。
     */
    public void assertCanPublish(Spu spu) {
        Category category = categoryRepository.findById(spu.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_NOT_FOUND,
                        "类目不存在: " + spu.getCategoryId()));
        if (!category.isEnabled()) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_NOT_FOUND,
                    "类目已禁用，无法上架商品: " + category.getName());
        }
        Brand brand = brandRepository.findById(spu.getBrandId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        "品牌不存在: " + spu.getBrandId()));
        if (!brand.isEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "品牌已禁用，无法上架商品: " + brand.getName());
        }
    }
}
