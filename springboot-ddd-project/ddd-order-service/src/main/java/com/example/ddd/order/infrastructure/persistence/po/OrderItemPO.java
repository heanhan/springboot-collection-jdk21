package com.example.ddd.order.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * PO：t_order_item 表映射（OrderItem 子实体）。
 */
@Entity
@Table(name = "t_order_item")
public class OrderItemPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "item_id", length = 32, nullable = false)
    private String itemId;

    @Column(name = "order_id", length = 32, nullable = false)
    private String orderId;

    @Column(name = "sku_id", length = 32, nullable = false)
    private String skuId;

    @Column(name = "spu_id", length = 32, nullable = false)
    private String spuId;

    @Column(name = "spu_name", length = 128, nullable = false)
    private String spuName;

    @Column(name = "sku_name", length = 255, nullable = false)
    private String skuName;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getSpuId() { return spuId; }
    public void setSpuId(String spuId) { this.spuId = spuId; }
    public String getSpuName() { return spuName; }
    public void setSpuName(String spuName) { this.spuName = spuName; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
