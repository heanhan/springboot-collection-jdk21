package com.example.ddd.product.interfaces.dto;

import java.math.BigDecimal;

/**
 * 请求 DTO：更新 SPU / 新增 SKU / 更新 SKU。
 * <p>用一个宽松 DTO 兼容"部分字段更新"，null 字段表示不修改。</p>
 */
public record UpdateSpuRequest(String name,
                               String subtitle,
                               String brandId,
                               String categoryId,
                               String mainImage,
                               String albumJson,
                               String detail) {
}
