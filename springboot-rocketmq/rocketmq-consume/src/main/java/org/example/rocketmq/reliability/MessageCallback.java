package org.example.rocketmq.reliability;

/**
 * 业务处理回调。
 *
 * <p><b>用途</b>：把「真正的业务逻辑」从可靠性框架中解耦出来。框架负责落库、幂等、
 * 失败记录与重试调度，业务只需实现本接口——处理成功则正常返回，处理失败则抛出异常。</p>
 *
 * <p>首次消费与数据库重试都会回调同一份逻辑，保证「重试即重放」。</p>
 *
 * @author demo
 */
@FunctionalInterface
public interface MessageCallback {

    /**
     * 执行业务处理。
     *
     * @param body 消息体（JSON 字符串）
     * @throws Exception 业务处理失败时抛出，框架据此记录失败并安排重试
     */
    void onMessage(String body) throws Exception;
}
