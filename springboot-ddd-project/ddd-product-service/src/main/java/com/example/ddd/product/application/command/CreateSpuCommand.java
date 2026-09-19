package com.example.ddd.product.application.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * 命令：创建 SPU（含初始 SKU 列表）。
 *
 * <p><b>为什么用 record？</b>Command 是不可变的输入数据袋，record 语义最贴切，
 * 且 IDE 自动生成 equals/hashCode/toString，方便日志。</p>
 */
public record CreateSpuCommand(String name,
                               String subtitle,
                               String brandId,
                               String categoryId,
                               String mainImage,
                               String albumJson,
                               String detail,
                               List<SkuItem> skus) {

    /**
     * SKU 明细项。
     */
    public record SkuItem(String skuName,
                          String specJson,
                          String image,
                          BigDecimal price,
                          BigDecimal marketPrice,
                          String skuCode,
                          String barcode,
                          Integer weightGram) {
    }
}
