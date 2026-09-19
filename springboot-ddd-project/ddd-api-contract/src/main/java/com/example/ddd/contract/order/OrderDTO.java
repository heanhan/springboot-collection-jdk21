package com.example.ddd.contract.order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单摘要 DTO。
 *
 * @param orderId         订单 ID
 * @param orderNo         订单号（对用户展示的短号）
 * @param userId          下单用户 ID
 * @param status          订单状态
 * @param totalAmount     商品总金额
 * @param shippingFee     运费
 * @param discountAmount  优惠金额
 * @param payAmount       实付金额 = totalAmount + shippingFee - discountAmount
 * @param shippingAddress 收货地址（拼接后字符串）
 * @param receiver        收件人
 * @param receiverMobile  收件人手机号
 * @param items           订单项
 * @param createdAt       下单时间
 */
public record OrderDTO(String orderId,
                       String orderNo,
                       String userId,
                       String status,
                       BigDecimal totalAmount,
                       BigDecimal shippingFee,
                       BigDecimal discountAmount,
                       BigDecimal payAmount,
                       String shippingAddress,
                       String receiver,
                       String receiverMobile,
                       List<OrderItemDTO> items,
                       LocalDateTime createdAt) implements Serializable {

    /**
     * 订单项 DTO。
     */
    public record OrderItemDTO(String itemId,
                               String skuId,
                               String spuName,
                               String skuName,
                               String image,
                               BigDecimal unitPrice,
                               int quantity,
                               BigDecimal subtotal) implements Serializable {
    }
}
