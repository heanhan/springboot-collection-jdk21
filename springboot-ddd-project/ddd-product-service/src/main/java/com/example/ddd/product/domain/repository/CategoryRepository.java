package com.example.ddd.product.domain.repository;

import com.example.ddd.product.domain.model.aggregate.Category;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Category。
 */
public interface CategoryRepository {

    Optional<Category> findById(String categoryId);

    List<Category> findChildren(String parentId);

    /** 按 code_path 前缀查子树，例如 "0/1/" 匹配 "0/1/11/"、"0/1/12/"。 */
    List<Category> findSubtree(String codePathPrefix);

    List<Category> findAllEnabled();

    void save(Category category);
}
