package com.example.ddd.inventory.domain.model.valueobject;

/**
 * 值对象：库存流水类型。
 *
 * <p><b>四种基本操作 + 一种人工调整：</b>
 * <ul>
 *   <li>{@link #LOCK}：预占。available -= qty, locked += qty。下单时。</li>
 *   <li>{@link #UNLOCK}：回滚预占。locked -= qty, available += qty。订单取消/超时。</li>
 *   <li>{@link #DEDUCT}：实扣。locked -= qty。支付成功后。</li>
 *   <li>{@link #ROLLBACK}：撤销实扣（退款场景）。available += qty。</li>
 *   <li>{@link #ADJUST}：人工调整（盘点/损耗）。</li>
 * </ul>
 */
public enum TransactionType {
    LOCK,
    UNLOCK,
    DEDUCT,
    ROLLBACK,
    ADJUST
}
