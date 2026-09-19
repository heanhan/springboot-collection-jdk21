package com.example.ddd.product.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 请求 DTO：创建 SPU（含初始 SKU）。
 */
public record CreateSpuRequest(
        @NotBlank(message = "SPU 名称不能为空")
        String name,
        String subtitle,
        @NotBlank(message = "brandId 不能为空")
        String brandId,
        @NotBlank(message = "categoryId 不能为空")
        String categoryId,
        String mainImage,
        String albumJson,
        String detail,
        @Valid
        List<SkuItem> skus) {

    public record SkuItem(
            @NotBlank(message = "SKU 名称不能为空")
            String skuName,
            String specJson,
            String image,
            @NotNull(message = "价格不能为空")
            @DecimalMin(value = "0.01", message = "价格必须大于 0")
            BigDecimal price,
            BigDecimal marketPrice,
            String skuCode,
            String barcode,
            Integer weightGram) {
    }
}
