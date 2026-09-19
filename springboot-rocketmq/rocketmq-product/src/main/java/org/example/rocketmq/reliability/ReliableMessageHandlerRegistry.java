package org.example.rocketmq.reliability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可靠消息处理器注册中心。
 *
 * <p><b>用途</b>：Spring 启动时自动收集所有 {@link ReliableMessageHandler} Bean，
 * 以 topic 为键建立索引。数据库重试调度器根据失败记录的 topic 反查对应处理器并重放业务。</p>
 *
 * <p>这样做的好处：新增一个可靠消费者时，只需实现 {@link ReliableMessageHandler}，
 * 无需修改调度器代码，符合开闭原则。</p>
 *
 * @author demo
 */
@Slf4j
@Component
public class ReliableMessageHandlerRegistry {

    /** topic -> 处理器 */
    private final Map<String, ReliableMessageHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * 通过构造器注入所有处理器 Bean（Spring 会把同一类型的 Bean 收集为 List）。
     *
     * @param handlers 所有实现了 ReliableMessageHandler 的 Bean
     */
    public ReliableMessageHandlerRegistry(List<ReliableMessageHandler> handlers) {
        for (ReliableMessageHandler handler : handlers) {
            ReliableMessageHandler exist = handlerMap.put(handler.supportTopic(), handler);
            if (exist != null) {
                // 同一 topic 注册了多个处理器，属于配置错误，及时暴露
                throw new IllegalStateException("Topic [" + handler.supportTopic()
                        + "] 存在多个 ReliableMessageHandler，请确保每个 topic 只有一个可靠处理器");
            }
            log.info("[可靠消息] 注册处理器: topic={}, handler={}",
                    handler.supportTopic(), handler.getClass().getSimpleName());
        }
    }

    /**
     * 按 topic 获取处理器。
     *
     * @param topic 消息主题
     * @return 对应处理器；不存在返回 null
     */
    public ReliableMessageHandler get(String topic) {
        return handlerMap.get(topic);
    }
}
