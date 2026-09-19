package org.example.rocketmq.enums;

/**
 * 消费端消息状态（mq_consume_record.status）。
 *
 * <p><b>用途</b>：标记一条消息在消费端的处理进度，是「幂等去重 + 消费状态追踪」的核心状态机。
 * 注意：消费失败的<b>重试调度</b>已统一迁移到 {@code mq_message_retry} 表（见 {@link RetryStatus}），
 * 本状态仅反映「最近一次消费结果」，用于幂等判断与审计。</p>
 *
 * <p><b>状态流转</b>：</p>
 * <pre>
 *   CONSUMING（接收消息即落库，正在处理）
 *      ├─ 业务成功 ──────────────▶ SUCCESS（幂等键已消费，重复投递将被跳过）
 *      └─ 业务失败 ──▶ FAILED（同时写入 mq_message_retry[type=CONSUME] 等待重试）
 *                          └─ 重试成功 ──▶ SUCCESS
 *                          └─ 重试次数达上限 ──▶ DEAD（死信，停止自动重试，转人工处理/告警）
 * </pre>
 *
 * @author demo
 */
public enum ConsumeStatus {

    /** 处理中：消息刚落库、业务逻辑正在执行 */
    CONSUMING,

    /** 消费成功：终态，重复投递时凭此状态做幂等跳过 */
    SUCCESS,

    /** 消费失败：已写入统一重试表等待重试 */
    FAILED,

    /** 死信：重试次数已达上限仍失败，终态，需人工介入 */
    DEAD
}
