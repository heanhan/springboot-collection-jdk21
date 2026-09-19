package com.example.ddd.contract.inventory.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：库存已实扣 (StockDeducted)。
 *
 * <p>支付成功后，把预占的库存转成实际售出。</p>
 *
 * @author ddd-learning
 */
public class StockDeductedEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String bizNo;
    private String transactionId;

    public StockDeductedEvent() {
        super();
    }

    public StockDeductedEvent(String warehouseId, String bizNo, String transactionId) {
        super(warehouseId);
        this.bizNo = bizNo;
        this.transactionId = transactionId;
    }

    public String getBizNo() { return bizNo; }
    public String getTransactionId() { return transactionId; }

    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
