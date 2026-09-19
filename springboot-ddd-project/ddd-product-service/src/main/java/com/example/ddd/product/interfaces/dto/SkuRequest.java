package com.example.ddd.product.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 请求 DTO：新增或修改 SKU。
 */
public record SkuRequest(
        @NotBlank(message = "skuName 不能为空")
        String skuName,
        String specJson,
        String image,
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,
        BigDecimal marketPrice,
        String skuCode,
        String barcode,
        Integer weightGram) {
}
