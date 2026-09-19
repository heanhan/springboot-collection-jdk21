package com.example.ddd.product.interfaces.assembler;

import com.example.ddd.contract.product.SkuDTO;
import com.example.ddd.contract.product.SpuDTO;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;

import java.util.List;

/**
 * Assembler：领域对象 → 契约 DTO 转换（用于 Feign 端点）。
 *
 * <p><b>为什么单独一个 Assembler？</b>
 * 契约 DTO ({@link SkuDTO} / {@link SpuDTO}) 位于 ddd-api-contract，
 * 是跨服务的"发布语言"，与领域模型的字段不完全一致（例如需要冗余 spuName）。
 * Assembler 负责"翻译"，让领域模型保持独立演进。</p>
 */
public final class ProductAssembler {

    private ProductAssembler() {
    }

    public static SkuDTO toContractDTO(Sku sku, String spuName) {
        return new SkuDTO(sku.getSkuId(), sku.getSpuId(), spuName,
                sku.getSkuName(), sku.getSpecJson(), sku.getImage(),
                sku.getPrice(), sku.getStatus().name());
    }

    public static SpuDTO toContractDTO(Spu spu, String brandName, String categoryName) {
        List<SkuDTO> skus = spu.getSkus().stream()
                .map(s -> toContractDTO(s, spu.getName()))
                .toList();
        return new SpuDTO(spu.getSpuId(), spu.getName(), spu.getBrandId(), brandName,
                spu.getCategoryId(), categoryName, spu.getMainImage(), spu.getDetail(),
                spu.getStatus().code(), skus);
    }
}
