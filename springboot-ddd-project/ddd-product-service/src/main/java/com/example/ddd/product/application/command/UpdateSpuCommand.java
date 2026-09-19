package com.example.ddd.product.application.command;

import java.math.BigDecimal;

/**
 * 命令：更新 SPU 基本信息。
 */
public record UpdateSpuCommand(String spuId,
                               String name,
                               String subtitle,
                               String brandId,
                               String categoryId,
                               String mainImage,
                               String albumJson,
                               String detail) {
}
