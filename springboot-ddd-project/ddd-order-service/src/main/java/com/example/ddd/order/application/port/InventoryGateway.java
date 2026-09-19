package com.example.ddd.order.application.port;

import java.util.List;

/**
 * 端口 (Port) / 防腐层 (ACL)：库存上下文网关。
 *
 * <p>订单下单时需要"预占"库存，取消 / 超时时需要"释放"库存。
 * 本接口用订单语言抽象这两个动作，屏蔽 {@code InventoryFeignClient} 的请求/响应 DTO 细节。</p>
 *
 * <p><b>失败语义：</b>预占失败（库存不足 / 服务不可用）应抛
 * {@link com.example.ddd.common.exception.BusinessException}，由应用服务决定回滚下单。</p>
 */
public interface InventoryGateway {

    /**
     * 预占库存。
     *
     * @param bizNo 业务号（幂等键），传 orderId
     * @param items 预占明细
     */
    void lock(String bizNo, List<LockItem> items);

    /**
     * 释放预占。
     *
     * @param bizNo  业务号
     * @param reason 释放原因
     */
    void release(String bizNo, String reason);

    /**
     * 值对象：预占明细项。
     *
     * @param skuId    SKU ID
     * @param quantity 数量
     */
    record LockItem(String skuId, int quantity) {
    }
}
