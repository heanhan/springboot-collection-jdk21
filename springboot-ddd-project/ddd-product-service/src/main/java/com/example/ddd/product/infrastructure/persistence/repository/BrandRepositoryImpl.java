package com.example.ddd.product.infrastructure.persistence.repository;

import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.repository.BrandRepository;
import com.example.ddd.product.infrastructure.persistence.converter.ProductConverter;
import com.example.ddd.product.infrastructure.persistence.dao.BrandDao;
import com.example.ddd.product.infrastructure.persistence.po.BrandPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Brand。
 */
@Repository
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandDao dao;

    public BrandRepositoryImpl(BrandDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Brand> findById(String brandId) {
        return dao.findByIdAndNotDeleted(brandId).map(ProductConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Brand> findByName(String name) {
        return dao.findByName(name).map(ProductConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return dao.existsByNameNotDeleted(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> findAllEnabled() {
        return dao.findAllEnabled().stream().map(ProductConverter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Brand brand) {
        BrandPO po = ProductConverter.toPO(brand);
        dao.findByIdAndNotDeleted(brand.getBrandId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        dao.save(po);
    }
}
