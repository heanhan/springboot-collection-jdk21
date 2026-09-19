package com.example.ddd.order.domain.model.entity;

import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.util.Objects;

/**
 * 实体：OrderItem 订单项（Order 聚合内的子实体）。
 *
 * <p><b>为什么是实体而不是值对象？</b>
 * 订单项有独立标识 {@code itemId}（用于售后、退款定位到具体某一行），
 * 虽然它不能脱离 Order 独立存在，但在聚合内部需要被单独引用，因此建模为实体。</p>
 *
 * <p><b>快照语义：</b>{@code spuName / skuName / unitPrice / image} 都是<b>下单那一刻</b>从商品服务拷贝的快照。
 * 之后商品改名、调价、下架都不影响历史订单——这是电商订单的基本原则。</p>
 *
 * <p><b>不变式：</b>quantity &gt; 0；subtotal == unitPrice × quantity。</p>
 */
public class OrderItem {

    private final String itemId;
    private final String skuId;
    private final String spuId;
    private final String spuName;
    private final String skuName;
    private final String image;
    private final Money unitPrice;
    private final int quantity;
    private final Money subtotal;

    public OrderItem(String itemId, String skuId, String spuId, String spuName, String skuName,
                     String image, Money unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单项数量必须大于 0");
        }
        Objects.requireNonNull(unitPrice, "unitPrice 不能为 null");
        this.itemId = Objects.requireNonNull(itemId);
        this.skuId = Objects.requireNonNull(skuId);
        this.spuId = spuId;
        this.spuName = spuName;
        this.skuName = skuName;
        this.image = image;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        // subtotal 由不变式派生，保证与 unitPrice * quantity 一致
        this.subtotal = unitPrice.multiply(quantity);
    }

    /** 重建（从持久化恢复，subtotal 直接回填，避免重复计算）。 */
    public static OrderItem reconstitute(String itemId, String skuId, String spuId, String spuName,
                                         String skuName, String image, Money unitPrice, int quantity,
                                         Money subtotal) {
        OrderItem item = new OrderItem(itemId, skuId, spuId, spuName, skuName, image, unitPrice, quantity);
        return item;
    }

    public String getItemId() { return itemId; }
    public String getSkuId() { return skuId; }
    public String getSpuId() { return spuId; }
    public String getSpuName() { return spuName; }
    public String getSkuName() { return skuName; }
    public String getImage() { return image; }
    public Money getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public Money getSubtotal() { return subtotal; }
}
