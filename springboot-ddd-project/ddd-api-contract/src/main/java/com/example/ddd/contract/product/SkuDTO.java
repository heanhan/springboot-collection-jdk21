package com.example.ddd.contract.product;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU DTO：库存最小单位 (Stock Keeping Unit)。
 *
 * <p><b>SPU vs SKU：</b>
 * SPU (Standard Product Unit) = 标准化产品单元，例如"iPhone 15 Pro"；
 * SKU = 库存量单位，例如"iPhone 15 Pro / 256G / 深空黑色"。
 * 一个 SPU 可以有多个 SKU，价格、库存都挂在 SKU 上。</p>
 *
 * @param skuId    SKU ID
 * @param spuId    所属 SPU ID
 * @param spuName  SPU 名称（下单时冗余到订单项，避免商品改名影响历史订单）
 * @param skuName  SKU 名称
 * @param specJson 规格 JSON，例如 {"颜色":"黑色","内存":"256G"}
 * @param image    主图 URL
 * @param price    售价（单位：元，scale=2）
 * @param status   状态：ON_SALE / OFF_SHELF
 */
public record SkuDTO(String skuId,
                     String spuId,
                     String spuName,
                     String skuName,
                     String specJson,
                     String image,
                     BigDecimal price,
                     String status) implements Serializable {
}
