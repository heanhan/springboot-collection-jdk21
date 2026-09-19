package org.example.rocketmq.reliability;

/**
 * 可靠消息业务处理器。
 *
 * <p><b>用途</b>：数据库重试时，调度器需要知道「某条失败消息该交给谁来重放」。
 * 每个使用可靠消费框架的消费者都实现本接口并注册为 Spring Bean，
 * 由 {@link ReliableMessageHandlerRegistry} 按 topic 收集，重试时按 topic 反查处理器。</p>
 *
 * @author demo
 */
public interface ReliableMessageHandler {

    /**
     * 该处理器负责的 Topic。
     *
     * @return topic 名称（与消费者监听的 topic 一致）
     */
    String supportTopic();

    /**
     * 重放业务：用数据库中保存的消息体重新执行一次业务逻辑。
     *
     * @param body 消息体（JSON 字符串）
     * @throws Exception 处理失败时抛出，框架据此累加重试次数或转死信
     */
    void handle(String body) throws Exception;
}
