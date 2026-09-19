package com.example.ddd.product.domain.repository;

import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;

import java.util.List;
import java.util.Optional;

/**
 * 仓储接口：Spu 聚合（依赖倒置）。
 *
 * <p><b>为什么接口在 domain 层，实现在 infrastructure 层？</b>
 * 这是 DDD 的<b>依赖倒置原则 (DIP)</b>：领域层定义"我需要什么能力"，
 * 基础设施层提供"具体怎么实现"，二者通过接口解耦。</p>
 */
public interface SpuRepository {

    Optional<Spu> findById(String spuId);

    /** 通过 skuId 反查所属 Spu 聚合（含全部 Sku）。 */
    Optional<Spu> findBySkuId(String skuId);

    /** 只加载 Sku 而不加载整个 Spu（读模型，性能优化）。 */
    Optional<Sku> findSkuById(String skuId);

    List<Sku> findSkusByIds(List<String> skuIds);

    List<Spu> findByCategory(String categoryId, String statusKeyword, int pageNum, int pageSize);

    long countByCategory(String categoryId, String statusKeyword);

    List<Spu> findOnSaleByCategory(String categoryId, int pageNum, int pageSize);

    void save(Spu spu);

    void deleteSku(String skuId);
}
