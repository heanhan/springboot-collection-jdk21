package org.example.rocketmq.enums;

/**
 * 本地消息表（mq_consume_record）的消费状态。
 *
 * <p><b>用途</b>：标记一条消息在消费端的处理进度，是「失败记录 + 数据库重试 + 幂等去重」的核心状态机。</p>
 *
 * <p><b>状态流转</b>：</p>
 * <pre>
 *   CONSUMING（首次落库，正在处理）
 *      ├─ 业务成功 ──────────────▶ SUCCESS（幂等键已消费，重复投递将被跳过）
 *      └─ 业务失败 ──▶ FAILED（retry_count+1，计算 next_retry_time，等待定时任务重试）
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

    /** 消费失败待重试：会被定时任务扫描并按退避策略重新投递 */
    FAILED,

    /** 死信：重试次数已达上限仍失败，终态，需人工介入 */
    DEAD
}
