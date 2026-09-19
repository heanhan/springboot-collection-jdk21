package com.example.ddd.product.interfaces.facade;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.product.SkuDTO;
import com.example.ddd.contract.product.SpuDTO;
import com.example.ddd.product.application.service.BrandApplicationService;
import com.example.ddd.product.application.service.CategoryApplicationService;
import com.example.ddd.product.application.service.SpuApplicationService;
import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;
import com.example.ddd.product.interfaces.assembler.ProductAssembler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Facade：实现 {@code com.example.ddd.contract.product.ProductFeignClient} 的服务端点。
 *
 * <p><b>路径必须与契约接口一致：</b>{@code /product/internal/**}。</p>
 */
@RestController
@RequestMapping("/product/internal")
public class ProductInternalFacade {

    private final SpuApplicationService spuService;
    private final BrandApplicationService brandService;
    private final CategoryApplicationService categoryService;

    public ProductInternalFacade(SpuApplicationService spuService,
                                 BrandApplicationService brandService,
                                 CategoryApplicationService categoryService) {
        this.spuService = spuService;
        this.brandService = brandService;
        this.categoryService = categoryService;
    }

    @GetMapping("/sku/{skuId}")
    public Result<SkuDTO> getSku(@PathVariable("skuId") String skuId) {
        Optional<Sku> opt = spuService.getSku(skuId);
        if (opt.isEmpty()) {
            return Result.fail("3002", "SKU 不存在: " + skuId);
        }
        Sku sku = opt.get();
        // 通过 sku.spuId 反查 SPU 拿 spuName（订单快照用）
        String spuName = spuService.getSpu(sku.getSpuId()).getName();
        return Result.ok(ProductAssembler.toContractDTO(sku, spuName));
    }

    @PostMapping("/sku/batch")
    public Result<List<SkuDTO>> listSkus(@RequestBody List<String> skuIds) {
        List<Sku> skus = spuService.listSkus(skuIds);
        List<SkuDTO> dtos = skus.stream().map(sku -> {
            String spuName = spuService.getSpu(sku.getSpuId()).getName();
            return ProductAssembler.toContractDTO(sku, spuName);
        }).toList();
        return Result.ok(dtos);
    }

    @GetMapping("/spu/{spuId}")
    public Result<SpuDTO> getSpu(@PathVariable("spuId") String spuId) {
        Spu spu = spuService.getSpu(spuId);
        String brandName = Optional.ofNullable(spu.getBrandId())
                .flatMap(id -> safeFindBrand(id)).map(Brand::getName).orElse(null);
        String categoryName = Optional.ofNullable(spu.getCategoryId())
                .flatMap(id -> safeFindCategory(id)).map(Category::getName).orElse(null);
        return Result.ok(ProductAssembler.toContractDTO(spu, brandName, categoryName));
    }

    private Optional<Brand> safeFindBrand(String brandId) {
        try {
            return Optional.of(brandService.getBrand(brandId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<Category> safeFindCategory(String categoryId) {
        try {
            return Optional.of(categoryService.getCategory(categoryId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
