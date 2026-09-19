package com.example.ddd.contract.inventory.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：库存已预占 (StockLocked)。
 *
 * <p><b>发布者：</b>inventory-service。</p>
 * <p><b>订阅者：</b>风控 / 报表服务（可选）。</p>
 *
 * @author ddd-learning
 */
public class StockLockedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    /** 业务号，通常是 orderId */
    private String bizNo;
    /** 库存流水 ID */
    private String transactionId;

    public StockLockedEvent() {
        super();
    }

    public StockLockedEvent(String warehouseId, String bizNo, String transactionId) {
        super(warehouseId);
        this.bizNo = bizNo;
        this.transactionId = transactionId;
    }

    public String getBizNo() { return bizNo; }
    public String getTransactionId() { return transactionId; }

    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
