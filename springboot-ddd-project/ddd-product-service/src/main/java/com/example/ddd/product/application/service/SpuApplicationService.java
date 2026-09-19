package com.example.ddd.product.application.service;

import com.example.ddd.common.domain.event.DomainEvent;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.product.application.command.AddSkuCommand;
import com.example.ddd.product.application.command.CreateSpuCommand;
import com.example.ddd.product.application.command.UpdateSkuCommand;
import com.example.ddd.product.application.command.UpdateSpuCommand;
import com.example.ddd.product.application.port.DomainEventPublisher;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.domain.model.entity.Sku;
import com.example.ddd.product.domain.model.valueobject.SkuStatus;
import com.example.ddd.product.domain.repository.SpuRepository;
import com.example.ddd.product.domain.service.ProductPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 应用服务：商品管理用例。
 *
 * <p><b>用例：</b>
 * <ul>
 *   <li>创建 SPU（草稿态，可含初始 SKU）</li>
 *   <li>更新 SPU / SKU</li>
 *   <li>新增 SKU</li>
 *   <li>删除 SKU</li>
 *   <li>上架 / 下架 SPU</li>
 * </ul>
 *
 * <p><b>事务边界：</b>每个写方法都是 {@code @Transactional}，
 * 事务提交后发布聚合根收集的领域事件。</p>
 */
@Service
public class SpuApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SpuApplicationService.class);

    private final SpuRepository spuRepository;
    private final ProductPublishService publishService;
    private final DomainEventPublisher eventPublisher;

    public SpuApplicationService(SpuRepository spuRepository,
                                 ProductPublishService publishService,
                                 DomainEventPublisher eventPublisher) {
        this.spuRepository = spuRepository;
        this.publishService = publishService;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // 用例 1：创建 SPU
    // ============================================================

    @Transactional
    public String createSpu(CreateSpuCommand cmd) {
        String spuId = IdGenerator.nextIdStr();
        Spu spu = Spu.create(spuId, cmd.name(), cmd.subtitle(), cmd.brandId(),
                cmd.categoryId(), cmd.mainImage(), cmd.albumJson(), cmd.detail());

        if (cmd.skus() != null) {
            for (CreateSpuCommand.SkuItem item : cmd.skus()) {
                Sku sku = new Sku(IdGenerator.nextIdStr(), spuId, item.skuName(),
                        item.specJson(), item.image(), item.price(), item.marketPrice(),
                        item.skuCode(), item.barcode(), SkuStatus.ON_SALE, item.weightGram());
                spu.addSku(sku);
            }
        }
        spuRepository.save(spu);
        log.info("[Product] SPU created spuId={} name={}", spuId, cmd.name());
        return spuId;
    }

    // ============================================================
    // 用例 2：更新 SPU 基本信息
    // ============================================================

    @Transactional
    public void updateSpu(UpdateSpuCommand cmd) {
        Spu spu = loadSpu(cmd.spuId());
        spu.updateInfo(cmd.name(), cmd.subtitle(), cmd.mainImage(),
                cmd.albumJson(), cmd.detail(), cmd.categoryId(), cmd.brandId());
        spuRepository.save(spu);
    }

    // ============================================================
    // 用例 3：新增 SKU
    // ============================================================

    @Transactional
    public String addSku(AddSkuCommand cmd) {
        Spu spu = loadSpu(cmd.spuId());
        Sku sku = new Sku(IdGenerator.nextIdStr(), spu.getSpuId(), cmd.skuName(),
                cmd.specJson(), cmd.image(), cmd.price(), cmd.marketPrice(),
                cmd.skuCode(), cmd.barcode(), SkuStatus.ON_SALE, cmd.weightGram());
        spu.addSku(sku);
        spuRepository.save(spu);
        return sku.getSkuId();
    }

    // ============================================================
    // 用例 4：更新 SKU
    // ============================================================

    @Transactional
    public void updateSku(UpdateSkuCommand cmd) {
        Spu spu = loadSpu(cmd.spuId());
        Sku sku = spu.findSku(cmd.skuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SKU_NOT_FOUND));
        sku.updateInfo(cmd.skuName(), cmd.specJson(), cmd.image(),
                cmd.marketPrice(), cmd.skuCode(), cmd.barcode(), cmd.weightGram());
        if (cmd.price() != null) {
            sku.changePrice(cmd.price());
        }
        spuRepository.save(spu);
    }

    // ============================================================
    // 用例 5：删除 SKU
    // ============================================================

    @Transactional
    public void removeSku(String spuId, String skuId) {
        Spu spu = loadSpu(spuId);
        spu.removeSku(skuId);
        spuRepository.save(spu);
        spuRepository.deleteSku(skuId);
    }

    // ============================================================
    // 用例 6：上架 / 下架
    // ============================================================

    @Transactional
    public void publish(String spuId) {
        Spu spu = loadSpu(spuId);
        // 领域服务：跨聚合校验（Category / Brand）
        publishService.assertCanPublish(spu);
        // 聚合根：内部规则校验 + 状态变更 + 事件收集
        spu.publish();
        spuRepository.save(spu);
        publishEvents(spu);
        log.info("[Product] SPU published spuId={}", spuId);
    }

    @Transactional
    public void offShelf(String spuId, String reason) {
        Spu spu = loadSpu(spuId);
        spu.offShelf(reason);
        spuRepository.save(spu);
        publishEvents(spu);
        log.info("[Product] SPU off-shelf spuId={} reason={}", spuId, reason);
    }

    // ============================================================
    // 用例 7：查询
    // ============================================================

    @Transactional(readOnly = true)
    public Spu getSpu(String spuId) {
        return loadSpu(spuId);
    }

    @Transactional(readOnly = true)
    public Optional<Sku> getSku(String skuId) {
        return spuRepository.findSkuById(skuId);
    }

    @Transactional(readOnly = true)
    public List<Sku> listSkus(List<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) return List.of();
        return spuRepository.findSkusByIds(skuIds);
    }

    @Transactional(readOnly = true)
    public List<Spu> listByCategory(String categoryId, String statusKeyword, int pageNum, int pageSize) {
        return spuRepository.findByCategory(categoryId, statusKeyword, pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public long countByCategory(String categoryId, String statusKeyword) {
        return spuRepository.countByCategory(categoryId, statusKeyword);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private Spu loadSpu(String spuId) {
        return spuRepository.findById(spuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SPU_NOT_FOUND));
    }

    private void publishEvents(Spu spu) {
        for (DomainEvent event : spu.getDomainEvents()) {
            try {
                eventPublisher.publish(event);
            } catch (Exception e) {
                log.error("[Product] publish event failed: {}", event, e);
            }
        }
        spu.clearDomainEvents();
    }
}
