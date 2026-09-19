package com.example.ddd.order.application.command;

import com.example.ddd.common.domain.valueobject.Address;

import java.util.List;

/**
 * 命令：下单 (Place Order)。
 *
 * <p><b>为什么 Address 直接进命令？</b>
 * 收货地址在下单时就被"快照"进订单，之后用户修改地址簿不影响本单。
 * interfaces 层的 assembler 负责把请求 DTO（可能是 addressId 或裸地址字段）解析成 {@link Address} 值对象。</p>
 *
 * @param userId          下单用户 ID
 * @param shippingAddress 收货地址（值对象）
 * @param remark          用户备注
 * @param items           下单明细
 */
public record PlaceOrderCommand(String userId,
                                Address shippingAddress,
                                String remark,
                                List<Item> items) {

    /**
     * 下单明细项。
     *
     * @param skuId    SKU ID
     * @param quantity 数量（&gt; 0）
     */
    public record Item(String skuId, int quantity) {
    }
}
