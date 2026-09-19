package com.example.ddd.contract.logistics;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发货单 DTO。
 *
 * @param shipmentId   发货单 ID
 * @param orderId      订单 ID
 * @param orderNo      订单号
 * @param userId       用户 ID
 * @param carrier      承运商：SF / YTO / ZTO / MOCK
 * @param trackingNo   运单号
 * @param status       状态：PENDING / SHIPPED / IN_TRANSIT / DELIVERED / REJECTED
 * @param receiver     收件人
 * @param receiverMobile 收件人电话
 * @param shippingAddress 收货地址
 * @param shippedAt    发货时间
 * @param deliveredAt  签收时间
 * @param tracks       物流轨迹
 */
public record ShipmentDTO(String shipmentId,
                          String orderId,
                          String orderNo,
                          String userId,
                          String carrier,
                          String trackingNo,
                          String status,
                          String receiver,
                          String receiverMobile,
                          String shippingAddress,
                          LocalDateTime shippedAt,
                          LocalDateTime deliveredAt,
                          List<Track> tracks) implements Serializable {

    /**
     * 单条物流轨迹。
     *
     * @param time        时间
     * @param location    地点
     * @param description 描述
     */
    public record Track(LocalDateTime time, String location, String description) implements Serializable {
    }
}
