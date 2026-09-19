package com.example.ddd.order.infrastructure.persistence.converter;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.order.domain.model.aggregate.Order;
import com.example.ddd.order.domain.model.entity.OrderItem;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;
import com.example.ddd.order.infrastructure.persistence.po.OrderItemPO;
import com.example.ddd.order.infrastructure.persistence.po.OrderPO;

import java.math.BigDecimal;
import java.util.List;

/**
 * PO ⇄ Domain 转换器。
 *
 * <p><b>关键翻译：</b>
 * <ul>
 *   <li>{@code BigDecimal} ⇄ {@code Money}（币种恒为 CNY）。</li>
 *   <li>多列地址 ⇄ {@code Address} 值对象。</li>
 *   <li>{@code String} ⇄ {@code OrderStatus} 枚举。</li>
 * </ul>
 * 领域对象重建走 {@code reconstitute} 静态工厂，绕过创建校验、不触发事件。</p>
 */
public final class OrderConverter {

    private OrderConverter() {}

    // ============================================================
    // Order
    // ============================================================

    public static Order toDomain(OrderPO po, List<OrderItemPO> itemPOs) {
        List<OrderItem> items = itemPOs.stream().map(OrderConverter::toDomain).toList();
        Address address = new Address(po.getShippingProvince(), po.getShippingCity(),
                po.getShippingDistrict(), po.getShippingDetail(), po.getShippingZipcode(),
                po.getReceiver(), po.getReceiverMobile());
        return Order.reconstitute(
                po.getOrderId(), po.getOrderNo(), po.getUserId(), items,
                toMoney(po.getTotalAmount()), toMoney(po.getShippingFee()),
                toMoney(po.getDiscountAmount()), toMoney(po.getPayAmount()),
                address, po.getRemark(), OrderStatus.valueOf(po.getStatus()),
                po.getCancelReason(), po.getPaymentChannel(), po.getPaymentTradeNo(),
                po.getCreatedAt(), po.getExpireAt(), po.getPaidAt(),
                po.getShippedAt(), po.getCompletedAt(), po.getCancelledAt());
    }

    public static OrderPO toPO(Order order) {
        OrderPO po = new OrderPO();
        po.setOrderId(order.getOrderId());
        po.setOrderNo(order.getOrderNo());
        po.setUserId(order.getUserId());
        po.setStatus(order.getStatus().name());
        po.setTotalAmount(order.getTotalAmount().amount());
        po.setShippingFee(order.getShippingFee().amount());
        po.setDiscountAmount(order.getDiscountAmount().amount());
        po.setPayAmount(order.getPayAmount().amount());
        Address addr = order.getShippingAddress();
        po.setReceiver(addr.receiver());
        po.setReceiverMobile(addr.mobile());
        po.setShippingProvince(addr.province());
        po.setShippingCity(addr.city());
        po.setShippingDistrict(addr.district());
        po.setShippingDetail(addr.detail());
        po.setShippingZipcode(addr.zipCode());
        po.setRemark(order.getRemark());
        po.setCancelReason(order.getCancelReason());
        po.setPaymentChannel(order.getPaymentChannel());
        po.setPaymentTradeNo(order.getPaymentTradeNo());
        po.setCreatedAt(order.getCreatedAt());
        po.setExpireAt(order.getExpireAt());
        po.setPaidAt(order.getPaidAt());
        po.setShippedAt(order.getShippedAt());
        po.setCompletedAt(order.getCompletedAt());
        po.setCancelledAt(order.getCancelledAt());
        return po;
    }

    // ============================================================
    // OrderItem
    // ============================================================

    public static OrderItem toDomain(OrderItemPO po) {
        return OrderItem.reconstitute(po.getItemId(), po.getSkuId(), po.getSpuId(),
                po.getSpuName(), po.getSkuName(), po.getImage(),
                toMoney(po.getUnitPrice()), po.getQuantity() == null ? 0 : po.getQuantity(),
                toMoney(po.getSubtotal()));
    }

    public static OrderItemPO toPO(OrderItem item, String orderId) {
        OrderItemPO po = new OrderItemPO();
        po.setItemId(item.getItemId());
        po.setOrderId(orderId);
        po.setSkuId(item.getSkuId());
        po.setSpuId(item.getSpuId());
        po.setSpuName(item.getSpuName());
        po.setSkuName(item.getSkuName());
        po.setImage(item.getImage());
        po.setUnitPrice(item.getUnitPrice().amount());
        po.setQuantity(item.getQuantity());
        po.setSubtotal(item.getSubtotal().amount());
        return po;
    }

    private static Money toMoney(BigDecimal amount) {
        return Money.ofCny(amount == null ? BigDecimal.ZERO : amount);
    }
}
