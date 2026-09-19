package com.example.ddd.product.interfaces.dto;

import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;

import java.math.BigDecimal;
import java.util.List;

/**
 * 响应 DTO：SPU 详情。
 */
public record SpuDetailResponse(String spuId,
                                String name,
                                String subtitle,
                                String brandId,
                                String categoryId,
                                String mainImage,
                                String albumJson,
                                String detail,
                                String status,
                                long salesCount,
                                List<SkuItem> skus) {

    public record SkuItem(String skuId,
                          String skuName,
                          String specJson,
                          String image,
                          BigDecimal price,
                          BigDecimal marketPrice,
                          String skuCode,
                          String barcode,
                          String status,
                          Integer weightGram) {
    }

    public static SpuDetailResponse from(Spu spu) {
        List<SkuItem> items = spu.getSkus().stream().map(SpuDetailResponse::toItem).toList();
        return new SpuDetailResponse(
                spu.getSpuId(), spu.getName(), spu.getSubtitle(),
                spu.getBrandId(), spu.getCategoryId(), spu.getMainImage(),
                spu.getAlbumJson(), spu.getDetail(), spu.getStatus().code(),
                spu.getSalesCount(), items);
    }

    private static SkuItem toItem(Sku sku) {
        return new SkuItem(sku.getSkuId(), sku.getSkuName(), sku.getSpecJson(), sku.getImage(),
                sku.getPrice(), sku.getMarketPrice(), sku.getSkuCode(), sku.getBarcode(),
                sku.getStatus().name(), sku.getWeightGram());
    }
}
