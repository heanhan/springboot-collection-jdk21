package com.example.ddd.product.application.command;

import java.math.BigDecimal;

/**
 * 命令：更新 SKU 信息 / 价格。
 */
public record UpdateSkuCommand(String spuId,
                               String skuId,
                               String skuName,
                               String specJson,
                               String image,
                               BigDecimal price,
                               BigDecimal marketPrice,
                               String skuCode,
                               String barcode,
                               Integer weightGram) {
}
