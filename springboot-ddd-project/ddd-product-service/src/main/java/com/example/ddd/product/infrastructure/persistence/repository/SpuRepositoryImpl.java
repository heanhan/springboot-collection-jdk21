package com.example.ddd.product.infrastructure.persistence.repository;

import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;
import com.example.ddd.product.domain.repository.SpuRepository;
import com.example.ddd.product.infrastructure.persistence.converter.ProductConverter;
import com.example.ddd.product.infrastructure.persistence.dao.SkuDao;
import com.example.ddd.product.infrastructure.persistence.dao.SpuDao;
import com.example.ddd.product.infrastructure.persistence.po.SkuPO;
import com.example.ddd.product.infrastructure.persistence.po.SpuPO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 仓储实现：Spu 聚合。
 *
 * <p><b>聚合装配策略：</b>
 * Spu 与其下所有 Sku 一起加载，保证聚合完整（避免 LazyInitialization 与部分状态）。
 * 由于 SKU 数量有限（通常 &lt; 50），一次性加载不会造成性能问题。</p>
 *
 * <p><b>保存策略：</b>
 * <ul>
 *   <li>SpuPO：按主键 upsert，保留 createTime / version。</li>
 *   <li>SkuPO：全量覆盖式保存（save all）。</li>
 *   <li>删除 Sku 走单独的 {@link #deleteSku(String)}（软删除）。</li>
 * </ul>
 */
@Repository
public class SpuRepositoryImpl implements SpuRepository {

    private final SpuDao spuDao;
    private final SkuDao skuDao;

    public SpuRepositoryImpl(SpuDao spuDao, SkuDao skuDao) {
        this.spuDao = spuDao;
        this.skuDao = skuDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Spu> findById(String spuId) {
        return spuDao.findByIdAndNotDeleted(spuId).map(po -> {
            List<SkuPO> skuPOs = skuDao.findBySpuId(spuId);
            return ProductConverter.toDomain(po, skuPOs);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Spu> findBySkuId(String skuId) {
        return skuDao.findByIdAndNotDeleted(skuId)
                .map(SkuPO::getSpuId)
                .flatMap(this::findById);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sku> findSkuById(String skuId) {
        return skuDao.findByIdAndNotDeleted(skuId).map(ProductConverter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sku> findSkusByIds(List<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return skuDao.findByIdIn(skuIds).stream().map(ProductConverter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Spu> findByCategory(String categoryId, String statusKeyword, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize);
        List<SpuPO> pos = spuDao.findByFilter(emptyToNull(categoryId), emptyToNull(statusKeyword), pageable);
        return loadAll(pos);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCategory(String categoryId, String statusKeyword) {
        return spuDao.countByFilter(emptyToNull(categoryId), emptyToNull(statusKeyword));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Spu> findOnSaleByCategory(String categoryId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize);
        return loadAll(spuDao.findOnSale(emptyToNull(categoryId), pageable));
    }

    @Override
    @Transactional
    public void save(Spu spu) {
        SpuPO po = ProductConverter.toPO(spu);
        spuDao.findByIdAndNotDeleted(spu.getSpuId()).ifPresent(existing -> {
            po.setCreateTime(existing.getCreateTime());
            po.setVersion(existing.getVersion());
        });
        spuDao.save(po);

        // 全量保存 SKU（新增/更新）
        for (Sku sku : spu.getSkus()) {
            SkuPO skuPO = ProductConverter.toPO(sku, spu.getName());
            skuDao.findByIdAndNotDeleted(sku.getSkuId()).ifPresent(existing -> {
                skuPO.setCreateTime(existing.getCreateTime());
                skuPO.setVersion(existing.getVersion());
            });
            skuDao.save(skuPO);
        }
    }

    @Override
    @Transactional
    public void deleteSku(String skuId) {
        skuDao.softDelete(skuId);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private List<Spu> loadAll(List<SpuPO> pos) {
        List<Spu> result = new ArrayList<>(pos.size());
        for (SpuPO po : pos) {
            List<SkuPO> skuPOs = skuDao.findBySpuId(po.getSpuId());
            result.add(ProductConverter.toDomain(po, skuPOs));
        }
        return result;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
