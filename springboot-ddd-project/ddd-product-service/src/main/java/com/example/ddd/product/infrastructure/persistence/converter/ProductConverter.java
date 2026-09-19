package com.example.ddd.product.infrastructure.persistence.converter;

import com.example.ddd.product.domain.model.aggregate.Brand;
import com.example.ddd.product.domain.model.aggregate.Category;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;
import com.example.ddd.product.domain.model.valueobject.SkuStatus;
import com.example.ddd.product.infrastructure.persistence.po.BrandPO;
import com.example.ddd.product.infrastructure.persistence.po.CategoryPO;
import com.example.ddd.product.infrastructure.persistence.po.SkuPO;
import com.example.ddd.product.infrastructure.persistence.po.SpuPO;

import java.util.List;

/**
 * PO ⇄ Domain 转换器。
 *
 * <p><b>为什么手写而不是 MapStruct？</b>
 * 学习项目更看重"看得懂转换过程"，手写能明确表达每个字段的映射意图，
 * 也便于在转换时插入业务判断（例如 status 字符串 → sealed 类型）。
 * 生产项目字段多时可用 MapStruct 自动生成。</p>
 */
public final class ProductConverter {

    private ProductConverter() {
    }

    // ============================================================
    // Spu
    // ============================================================

    public static Spu toDomain(SpuPO po, List<SkuPO> skuPOs) {
        List<Sku> skus = skuPOs == null ? List.of() : skuPOs.stream().map(ProductConverter::toDomain).toList();
        return Spu.reconstitute(
                po.getSpuId(), po.getName(), po.getSubtitle(),
                po.getBrandId(), po.getCategoryId(), po.getMainImage(),
                po.getAlbumJson(), po.getDetail(),
                po.getStatus(), po.getSalesCount() == null ? 0L : po.getSalesCount(),
                po.getPublishTime(), po.getOffShelfReason(), skus);
    }

    public static SpuPO toPO(Spu spu) {
        SpuPO po = new SpuPO();
        po.setSpuId(spu.getSpuId());
        po.setName(spu.getName());
        po.setSubtitle(spu.getSubtitle());
        po.setBrandId(spu.getBrandId());
        po.setCategoryId(spu.getCategoryId());
        po.setMainImage(spu.getMainImage());
        po.setAlbumJson(spu.getAlbumJson());
        po.setDetail(spu.getDetail());
        po.setStatus(spu.getStatus().code());
        po.setSalesCount(spu.getSalesCount());
        po.setPublishTime(spu.getPublishTime());
        po.setOffShelfReason(spu.getOffShelfReason());
        return po;
    }

    // ============================================================
    // Sku
    // ============================================================

    public static Sku toDomain(SkuPO po) {
        return new Sku(po.getSkuId(), po.getSpuId(), po.getSkuName(),
                po.getSpecJson(), po.getImage(), po.getPrice(), po.getMarketPrice(),
                po.getSkuCode(), po.getBarcode(),
                SkuStatus.valueOf(po.getStatus()), po.getWeightGram());
    }

    public static SkuPO toPO(Sku sku, String spuName) {
        SkuPO po = new SkuPO();
        po.setSkuId(sku.getSkuId());
        po.setSpuId(sku.getSpuId());
        po.setSpuName(spuName);
        po.setSkuName(sku.getSkuName());
        po.setSpecJson(sku.getSpecJson());
        po.setImage(sku.getImage());
        po.setPrice(sku.getPrice());
        po.setMarketPrice(sku.getMarketPrice());
        po.setSkuCode(sku.getSkuCode());
        po.setBarcode(sku.getBarcode());
        po.setStatus(sku.getStatus().name());
        po.setWeightGram(sku.getWeightGram());
        return po;
    }

    // ============================================================
    // Category
    // ============================================================

    public static Category toDomain(CategoryPO po) {
        return Category.reconstitute(po.getCategoryId(), po.getParentId(), po.getCodePath(),
                po.getName(), po.getLevel() == null ? 1 : po.getLevel(),
                po.getSort() == null ? 0 : po.getSort(), po.getIcon(),
                "ACTIVE".equals(po.getStatus()));
    }

    public static CategoryPO toPO(Category c) {
        CategoryPO po = new CategoryPO();
        po.setCategoryId(c.getCategoryId());
        po.setParentId(c.getParentId());
        po.setCodePath(c.getCodePath());
        po.setName(c.getName());
        po.setLevel(c.getLevel());
        po.setSort(c.getSort());
        po.setIcon(c.getIcon());
        po.setStatus(c.isEnabled() ? "ACTIVE" : "DISABLED");
        return po;
    }

    // ============================================================
    // Brand
    // ============================================================

    public static Brand toDomain(BrandPO po) {
        return Brand.reconstitute(po.getBrandId(), po.getName(), po.getLogo(), po.getStory(),
                po.getFirstLetter(), po.getSort() == null ? 0 : po.getSort(),
                "ACTIVE".equals(po.getStatus()));
    }

    public static BrandPO toPO(Brand b) {
        BrandPO po = new BrandPO();
        po.setBrandId(b.getBrandId());
        po.setName(b.getName());
        po.setLogo(b.getLogo());
        po.setStory(b.getStory());
        po.setFirstLetter(b.getFirstLetter());
        po.setSort(b.getSort());
        po.setStatus(b.isEnabled() ? "ACTIVE" : "DISABLED");
        return po;
    }
}
