package org.example.rocketmq.enums;

/**
 * 生产端消息状态（mq_produce_record.produce_status）。
 *
 * <p><b>用途</b>：标记一条消息在「生产端」的处理阶段，配合 {@link ConsumeStatus}
 * 完整还原一条消息的生命周期。</p>
 *
 * <p><b>状态流转</b>：</p>
 * <pre>
 *   PENDING（先落库，再调用 broker 发送 —— 本地消息表发送模式）
 *      ├─ broker 返回 SEND_OK ──▶ SUCCESS（携带 msgId）
 *      └─ 发送抛异常 ─────────▶ FAILED（记录错误，可由业务/定时任务补偿重发）
 * </pre>
 *
 * <p><b>为什么要先落库再发送</b>：即便发送过程中应用宕机，PENDING 记录也已存在，
 * 后续可通过扫描 PENDING 且创建时间过久的记录进行补偿发送，避免消息丢失。</p>
 *
 * @author demo
 */
public enum ProduceStatus {

    /** 已落库、尚未确认发送结果（发送中或应用宕机） */
    PENDING,

    /** 发送成功：broker 已接收，msgId 已回填 */
    SUCCESS,

    /** 发送失败：broker 拒绝、网络异常、超时等 */
    FAILED
}
