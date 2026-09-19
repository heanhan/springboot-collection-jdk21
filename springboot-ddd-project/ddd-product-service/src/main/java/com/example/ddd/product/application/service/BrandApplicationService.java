package com.example.ddd.product.application.service;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.repository.BrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用服务：品牌管理。
 */
@Service
public class BrandApplicationService {

    private final BrandRepository brandRepository;

    public BrandApplicationService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public String createBrand(String name, String logo, String story, int sort) {
        if (brandRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "品牌名已存在: " + name);
        }
        String brandId = IdGenerator.nextIdStr();
        Brand brand = Brand.create(brandId, name, logo, story, sort);
        brandRepository.save(brand);
        return brandId;
    }

    @Transactional
    public void updateBrand(String brandId, String name, String logo, String story, Integer sort) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_BRAND_NOT_FOUND));
        if (name != null && !name.equals(brand.getName())) {
            if (brandRepository.existsByName(name)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "品牌名已存在: " + name);
            }
            brand.rename(name);
        }
        brand.updateInfo(logo, story, sort);
        brandRepository.save(brand);
    }

    @Transactional
    public void disable(String brandId) {
        Brand b = brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_BRAND_NOT_FOUND));
        b.disable();
        brandRepository.save(b);
    }

    @Transactional(readOnly = true)
    public Brand getBrand(String brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_BRAND_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Brand> listAll() {
        return brandRepository.findAllEnabled();
    }
}
