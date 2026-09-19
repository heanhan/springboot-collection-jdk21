package com.example.ddd.contract.inventory.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：库存预占已释放 (StockReleased)。
 *
 * <p><b>发布者：</b>inventory-service。</p>
 * <p><b>触发场景：</b>订单取消 / 超时未支付，把 locked_qty 归还给 available_qty。</p>
 * <p><b>订阅者：</b>order-service（可用于对账）、报表服务。</p>
 *
 * @author ddd-learning
 */
public class StockReleasedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    /** 业务号，通常是 orderId */
    private String bizNo;
    /** 释放原因，例如 ORDER_CANCELLED / ORDER_TIMEOUT */
    private String reason;

    public StockReleasedEvent() {
        super();
    }

    public StockReleasedEvent(String warehouseId, String bizNo, String reason) {
        super(warehouseId);
        this.bizNo = bizNo;
        this.reason = reason;
    }

    public String getBizNo() { return bizNo; }
    public String getReason() { return reason; }

    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public void setReason(String reason) { this.reason = reason; }
}
