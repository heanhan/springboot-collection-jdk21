package com.example.ddd.contract.payment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 DTO。
 *
 * @param paymentId    支付单 ID
 * @param orderId      关联订单 ID
 * @param orderNo      关联订单号
 * @param userId       用户 ID
 * @param amount       支付金额
 * @param channel      渠道：ALIPAY / WECHAT / MOCK
 * @param status       状态：PENDING / SUCCESS / FAILED / CLOSED / REFUNDING / REFUNDED
 * @param tradeNo      渠道流水号（支付成功后填充）
 * @param paidAt       支付时间
 * @param createdAt    创建时间
 */
public record PaymentOrderDTO(String paymentId,
                              String orderId,
                              String orderNo,
                              String userId,
                              BigDecimal amount,
                              String channel,
                              String status,
                              String tradeNo,
                              LocalDateTime paidAt,
                              LocalDateTime createdAt) implements Serializable {
}
