package com.example.ddd.product.application.service;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用服务：类目管理。
 */
@Service
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public String createCategory(String parentId, String name, int sort, String icon) {
        String categoryId = IdGenerator.nextIdStr();
        String parentCodePath = "0/";
        int parentLevel = 0;
        if (parentId != null && !Category.ROOT_ID.equals(parentId)) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_NOT_FOUND,
                            "父类目不存在: " + parentId));
            parentCodePath = parent.getCodePath();
            parentLevel = parent.getLevel();
        }
        Category category = Category.create(categoryId, parentId == null ? Category.ROOT_ID : parentId,
                parentCodePath, parentLevel, name, sort, icon);
        categoryRepository.save(category);
        return categoryId;
    }

    @Transactional
    public void rename(String categoryId, String newName) {
        Category c = loadCategory(categoryId);
        c.rename(newName);
        categoryRepository.save(c);
    }

    @Transactional
    public void disable(String categoryId) {
        Category c = loadCategory(categoryId);
        c.disable();
        categoryRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Category getCategory(String categoryId) {
        return loadCategory(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Category> listChildren(String parentId) {
        return categoryRepository.findChildren(parentId == null ? Category.ROOT_ID : parentId);
    }

    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAllEnabled();
    }

    private Category loadCategory(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_CATEGORY_NOT_FOUND));
    }
}
