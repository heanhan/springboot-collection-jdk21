package com.example.ddd.contract.cart;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目 DTO。
 *
 * @param itemId    条目 ID
 * @param userId    所属用户
 * @param skuId     SKU ID
 * @param spuName   SPU 名称（冗余展示）
 * @param skuName   SKU 名称
 * @param image     图片
 * @param price     加入时单价（可能与实际售价不一致，需下单时刷新）
 * @param quantity  数量
 * @param checked   是否选中结算
 */
public record CartItemDTO(String itemId,
                          String userId,
                          String skuId,
                          String spuName,
                          String skuName,
                          String image,
                          BigDecimal price,
                          int quantity,
                          boolean checked) implements Serializable {
}
