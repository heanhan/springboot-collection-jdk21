package com.example.ddd.product.interfaces.rest;

import com.example.ddd.common.result.Result;
import com.example.ddd.product.application.service.BrandApplicationService;
import com.example.ddd.product.application.service.CategoryApplicationService;
import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.interfaces.dto.CatalogResponses.BrandResponse;
import com.example.ddd.product.interfaces.dto.CatalogResponses.CategoryResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：类目 + 品牌。
 *
 * <p>合并到一个 Controller 是因为二者都是"字典型"数据，接口简单、访问模式相似。</p>
 */
@RestController
@RequestMapping("/catalog")
@Validated
public class CatalogController {

    private final CategoryApplicationService categoryService;
    private final BrandApplicationService brandService;

    public CatalogController(CategoryApplicationService categoryService, BrandApplicationService brandService) {
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    // ============================================================
    // Category
    // ============================================================

    @PostMapping("/categories")
    public Result<String> createCategory(@RequestBody CategoryRequest req) {
        String id = categoryService.createCategory(req.parentId(), req.name(),
                req.sort() == null ? 0 : req.sort(), req.icon());
        return Result.ok(id);
    }

    @GetMapping("/categories")
    public Result<List<CategoryResponse>> listCategories(@RequestParam(required = false) String parentId) {
        List<Category> list = parentId == null ? categoryService.listAll() : categoryService.listChildren(parentId);
        return Result.ok(list.stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/categories/{categoryId}")
    public Result<CategoryResponse> getCategory(@PathVariable String categoryId) {
        return Result.ok(CategoryResponse.from(categoryService.getCategory(categoryId)));
    }

    @PutMapping("/categories/{categoryId}")
    public Result<Void> renameCategory(@PathVariable String categoryId,
                                       @RequestParam @NotBlank String name) {
        categoryService.rename(categoryId, name);
        return Result.ok();
    }

    @PostMapping("/categories/{categoryId}/disable")
    public Result<Void> disableCategory(@PathVariable String categoryId) {
        categoryService.disable(categoryId);
        return Result.ok();
    }

    // ============================================================
    // Brand
    // ============================================================

    @PostMapping("/brands")
    public Result<String> createBrand(@RequestBody BrandRequest req) {
        String id = brandService.createBrand(req.name(), req.logo(), req.story(),
                req.sort() == null ? 0 : req.sort());
        return Result.ok(id);
    }

    @GetMapping("/brands")
    public Result<List<BrandResponse>> listBrands() {
        List<Brand> list = brandService.listAll();
        return Result.ok(list.stream().map(BrandResponse::from).toList());
    }

    @GetMapping("/brands/{brandId}")
    public Result<BrandResponse> getBrand(@PathVariable String brandId) {
        return Result.ok(BrandResponse.from(brandService.getBrand(brandId)));
    }

    @PutMapping("/brands/{brandId}")
    public Result<Void> updateBrand(@PathVariable String brandId, @RequestBody BrandRequest req) {
        brandService.updateBrand(brandId, req.name(), req.logo(), req.story(), req.sort());
        return Result.ok();
    }

    @PostMapping("/brands/{brandId}/disable")
    public Result<Void> disableBrand(@PathVariable String brandId) {
        brandService.disable(brandId);
        return Result.ok();
    }

    // ============================================================
    // 请求 DTO
    // ============================================================

    public record CategoryRequest(String parentId, String name, Integer sort, String icon) {
    }

    public record BrandRequest(String name, String logo, String story, Integer sort) {
    }
}
