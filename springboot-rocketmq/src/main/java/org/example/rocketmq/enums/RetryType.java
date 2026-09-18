package org.example.rocketmq.enums;

/**
 * 重试消息类型（mq_message_retry.retry_type）。
 *
 * <p><b>用途</b>：统一重试表同时承载「生产失败」与「消费失败」两类消息，
 * 用本枚举区分，便于重试调度器按类型走不同的重放路径：</p>
 * <ul>
 *     <li>{@link #PRODUCE}：生产者发送失败，重试时取原始报文<b>重新发送</b>（再生产一次）；</li>
 *     <li>{@link #CONSUME}：消费者处理失败，重试时取原始报文<b>重新执行业务</b>（再消费一次）。</li>
 * </ul>
 *
 * @author demo
 */
public enum RetryType {

    /** 生产失败：发送 broker 失败，需重新生产（重发原始消息） */
    PRODUCE,

    /** 消费失败：业务处理失败，需重新消费（重放业务逻辑） */
    CONSUME
}
