package com.example.ddd.contract.product;

import com.example.ddd.common.result.Result;
import com.example.ddd.contract.ServiceNames;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商品服务对外 Feign 契约。
 *
 * <p>主要供订单、购物车、库存服务查询商品/SKU 详情。</p>
 *
 * @author ddd-learning
 */
@FeignClient(name = ServiceNames.PRODUCT, contextId = "productFeignClient", path = "/product/internal",
        url = "${ddd.services.product-url:}")
public interface ProductFeignClient {

    /**
     * 根据 skuId 查询 SKU 详情（下单前必查）。
     */
    @GetMapping("/sku/{skuId}")
    Result<SkuDTO> getSku(@PathVariable("skuId") String skuId);

    /**
     * 批量查询 SKU 详情（购物车结算、批量下单）。
     */
    @PostMapping("/sku/batch")
    Result<List<SkuDTO>> listSkus(@RequestBody List<String> skuIds);

    /**
     * 根据 spuId 查询 SPU 详情（含所有 SKU）。
     */
    @GetMapping("/spu/{spuId}")
    Result<SpuDTO> getSpu(@PathVariable("spuId") String spuId);
}
