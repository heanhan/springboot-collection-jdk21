package com.example.ddd.contract.order.event;

import com.example.ddd.common.domain.event.AbstractDomainEvent;

/**
 * 领域事件：订单已取消 (OrderCancelled)。
 *
 * <p><b>触发场景：</b>
 * <ul>
 *   <li>用户主动取消未支付订单</li>
 *   <li>定时任务发现订单超时未支付</li>
 *   <li>支付失败次数超限</li>
 * </ul>
 *
 * <p><b>订阅者：</b>inventory-service（释放预占）、payment-service（关闭未支付的支付单）。</p>
 *
 * @author ddd-learning
 */
public class OrderCancelledEvent extends AbstractDomainEvent {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private String userId;
    /** 取消原因码：USER_CANCEL / TIMEOUT / PAY_FAILED / OUT_OF_STOCK */
    private String reasonCode;
    /** 取消原因描述 */
    private String reasonDesc;

    public OrderCancelledEvent() {
        super();
    }

    public OrderCancelledEvent(String orderId, String orderNo, String userId,
                               String reasonCode, String reasonDesc) {
        super(orderId);
        this.orderNo = orderNo;
        this.userId = userId;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
    }

    public String getOrderNo() { return orderNo; }
    public String getUserId() { return userId; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonDesc() { return reasonDesc; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public void setReasonDesc(String reasonDesc) { this.reasonDesc = reasonDesc; }
}
