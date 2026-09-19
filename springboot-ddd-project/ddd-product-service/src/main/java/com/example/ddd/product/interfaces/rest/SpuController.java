package com.example.ddd.product.interfaces.rest;

import com.example.ddd.common.result.PageResult;
import com.example.ddd.common.result.Result;
import com.example.ddd.product.application.command.AddSkuCommand;
import com.example.ddd.product.application.command.CreateSpuCommand;
import com.example.ddd.product.application.command.UpdateSkuCommand;
import com.example.ddd.product.application.command.UpdateSpuCommand;
import com.example.ddd.product.application.service.SpuApplicationService;
import com.example.ddd.product.domain.model.aggregate.Spu;
import com.example.ddd.product.interfaces.dto.CreateSpuRequest;
import com.example.ddd.product.interfaces.dto.SkuRequest;
import com.example.ddd.product.interfaces.dto.SpuDetailResponse;
import com.example.ddd.product.interfaces.dto.UpdateSpuRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：SPU 商品管理（管理端 + 用户端）。
 */
@RestController
@RequestMapping("/spus")
public class SpuController {

    private final SpuApplicationService spuService;

    public SpuController(SpuApplicationService spuService) {
        this.spuService = spuService;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateSpuRequest req) {
        List<CreateSpuCommand.SkuItem> items = req.skus() == null ? List.of()
                : req.skus().stream().map(s -> new CreateSpuCommand.SkuItem(
                        s.skuName(), s.specJson(), s.image(), s.price(), s.marketPrice(),
                        s.skuCode(), s.barcode(), s.weightGram())).toList();
        String spuId = spuService.createSpu(new CreateSpuCommand(
                req.name(), req.subtitle(), req.brandId(), req.categoryId(),
                req.mainImage(), req.albumJson(), req.detail(), items));
        return Result.ok(spuId, "创建成功");
    }

    @GetMapping("/{spuId}")
    public Result<SpuDetailResponse> get(@PathVariable String spuId) {
        Spu spu = spuService.getSpu(spuId);
        return Result.ok(SpuDetailResponse.from(spu));
    }

    @PutMapping("/{spuId}")
    public Result<Void> update(@PathVariable String spuId, @RequestBody UpdateSpuRequest req) {
        spuService.updateSpu(new UpdateSpuCommand(spuId, req.name(), req.subtitle(),
                req.brandId(), req.categoryId(), req.mainImage(), req.albumJson(), req.detail()));
        return Result.ok();
    }

    @PostMapping("/{spuId}/publish")
    public Result<Void> publish(@PathVariable String spuId) {
        spuService.publish(spuId);
        return Result.ok(null, "上架成功");
    }

    @PostMapping("/{spuId}/off-shelf")
    public Result<Void> offShelf(@PathVariable String spuId,
                                 @RequestParam(required = false, defaultValue = "manual") String reason) {
        spuService.offShelf(spuId, reason);
        return Result.ok(null, "下架成功");
    }

    @GetMapping
    public Result<PageResult<SpuDetailResponse>> list(@RequestParam(required = false) String categoryId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        List<Spu> spus = spuService.listByCategory(categoryId, status, pageNum, pageSize);
        long total = spuService.countByCategory(categoryId, status);
        List<SpuDetailResponse> items = spus.stream().map(SpuDetailResponse::from).toList();
        return Result.ok(new PageResult<>(items, total, pageNum, pageSize, 0));
    }

    // ============================================================
    // SKU 管理
    // ============================================================

    @PostMapping("/{spuId}/skus")
    public Result<String> addSku(@PathVariable String spuId, @Valid @RequestBody SkuRequest req) {
        String skuId = spuService.addSku(new AddSkuCommand(spuId, req.skuName(), req.specJson(),
                req.image(), req.price(), req.marketPrice(), req.skuCode(), req.barcode(), req.weightGram()));
        return Result.ok(skuId);
    }

    @PutMapping("/{spuId}/skus/{skuId}")
    public Result<Void> updateSku(@PathVariable String spuId, @PathVariable String skuId,
                                  @RequestBody SkuRequest req) {
        spuService.updateSku(new UpdateSkuCommand(spuId, skuId, req.skuName(), req.specJson(),
                req.image(), req.price(), req.marketPrice(), req.skuCode(), req.barcode(), req.weightGram()));
        return Result.ok();
    }

    @DeleteMapping("/{spuId}/skus/{skuId}")
    public Result<Void> removeSku(@PathVariable String spuId, @PathVariable String skuId) {
        spuService.removeSku(spuId, skuId);
        return Result.ok();
    }
}
