package org.example.rocketmq.enums;

/**
 * 重试记录状态（mq_message_retry.status）。
 *
 * <p><b>状态流转</b>：</p>
 * <pre>
 *   PENDING（已入重试表，等待到期重试）
 *      ├─ 调度器抢占 ──▶ RETRYING（正在重试，多实例下用乐观更新抢占，避免重复重试）
 *      │                    ├─ 重试成功 ──▶ SUCCESS（终态）
 *      │                    └─ 重试失败 ──▶ retry_count+1，未达上限回到 PENDING 并排期 next_retry_time
 *      └─ retry_count ≥ max_retry ──▶ DEAD（死信，终态，需人工介入）
 * </pre>
 *
 * @author demo
 */
public enum RetryStatus {

    /** 待重试：已入表，等待 next_retry_time 到期后被调度器捞取 */
    PENDING,

    /** 重试中：已被调度器抢占、正在执行重放（多实例并发控制的中间态） */
    RETRYING,

    /** 重试成功：终态 */
    SUCCESS,

    /** 死信：重试达上限仍失败，终态，需人工处理/告警 */
    DEAD
}
