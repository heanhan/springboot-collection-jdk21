package com.example.ddd.product.infrastructure.persistence.repository;

import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.domain.repository.CategoryRepository;
import com.example.ddd.product.infrastructure.persistence.converter.ProductConverter;
import com.example.ddd.product.infrastructure.persistence.dao.CategoryDao;
import com.example.ddd.product.infrastructure.persistence.po.CategoryPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Category。
 */
@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryDao dao;

    public CategoryRepositoryImpl(CategoryDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(String categoryId) {
        return dao.findByIdAndNotDeleted(categoryId).map(ProductConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findChildren(String parentId) {
        return dao.findByParentId(parentId).stream().map(ProductConverter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findSubtree(String codePathPrefix) {
        return dao.findSubtree(codePathPrefix).stream().map(ProductConverter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllEnabled() {
        return dao.findAllEnabled().stream().map(ProductConverter::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Category category) {
        CategoryPO po = ProductConverter.toPO(category);
        dao.findByIdAndNotDeleted(category.getCategoryId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        dao.save(po);
    }
}
