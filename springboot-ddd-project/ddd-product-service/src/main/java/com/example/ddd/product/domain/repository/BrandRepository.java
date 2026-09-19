package com.example.ddd.product.domain.repository;

import com.example.ddd.product.domain.model.aggregate.Brand;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Brand。
 */
public interface BrandRepository {

    Optional<Brand> findById(String brandId);

    Optional<Brand> findByName(String name);

    boolean existsByName(String name);

    List<Brand> findAllEnabled();

    void save(Brand brand);
}
