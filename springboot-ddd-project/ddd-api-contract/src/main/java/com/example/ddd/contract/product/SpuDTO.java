package com.example.ddd.contract.product;

import java.io.Serializable;
import java.util.List;

/**
 * SPU DTO：标准化产品单元。
 *
 * @param spuId     SPU ID
 * @param name      名称
 * @param brandId   品牌 ID
 * @param brandName 品牌名（冗余，便于展示）
 * @param categoryId   类目 ID
 * @param categoryName 类目名（冗余）
 * @param mainImage 主图
 * @param detail    详情（富文本 HTML）
 * @param status    状态：DRAFT / ON_SALE / OFF_SHELF
 * @param skus      该 SPU 下的所有 SKU
 */
public record SpuDTO(String spuId,
                     String name,
                     String brandId,
                     String brandName,
                     String categoryId,
                     String categoryName,
                     String mainImage,
                     String detail,
                     String status,
                     List<SkuDTO> skus) implements Serializable {
}
