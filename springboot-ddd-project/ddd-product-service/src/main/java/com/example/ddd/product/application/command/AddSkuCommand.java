package com.example.ddd.product.application.command;

import java.math.BigDecimal;

/**
 * 命令：新增 SKU 到已有 SPU。
 */
public record AddSkuCommand(String spuId,
                            String skuName,
                            String specJson,
                            String image,
                            BigDecimal price,
                            BigDecimal marketPrice,
                            String skuCode,
                            String barcode,
                            Integer weightGram) {
}
