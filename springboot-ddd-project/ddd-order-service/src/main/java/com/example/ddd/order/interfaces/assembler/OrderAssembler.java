package com.example.ddd.order.interfaces.assembler;

import com.example.ddd.contract.order.OrderDTO;
import com.example.ddd.order.domain.model.aggregate.Order;

/**
 * 接口层装配器：Order 聚合 → OrderDTO（对齐其他服务的 {@code *Assembler} 约定）。
 *
 * <p>聚合到发布语言的单向转换只允许出现在接口层，Controller 内不再手写映射。</p>
 */
public final class OrderAssembler {

    private OrderAssembler() {}

    public static OrderDTO toDTO(Order o) {
        return new OrderDTO(o.getOrderId(), o.getOrderNo(), o.getUserId(), o.getStatus().name(),
                o.getTotalAmount().amount(), o.getShippingFee().amount(), o.getDiscountAmount().amount(), o.getPayAmount().amount(),
                o.getShippingAddress().fullAddress(), o.getShippingAddress().receiver(), o.getShippingAddress().mobile(),
                o.getItems().stream().map(i -> new OrderDTO.OrderItemDTO(i.getItemId(), i.getSkuId(), i.getSpuName(),
                        i.getSkuName(), i.getImage(), i.getUnitPrice().amount(), i.getQuantity(), i.getSubtotal().amount())).toList(), o.getCreatedAt());
    }
}
