package org.example.rocketmq.common;

/**
 * RocketMQ 全局常量。
 *
 * <p><b>用途</b>：集中管理所有 Topic、消费者组（ConsumerGroup）、生产者组、Tag 等标识，
 * 方便统一修改，避免字符串散落在各个类中导致难以维护。</p>
 *
 * <p><b>命名约定</b>：</p>
 * <ul>
 *     <li>Topic 以 {@code demo-} 前缀命名，按业务场景区分，互不干扰；</li>
 *     <li>每个消费者使用<b>独立</b>的 ConsumerGroup（RocketMQ 要求同组内消费逻辑一致，
 *         不同场景务必使用不同组名，否则会互相影响消费位点）。</li>
 * </ul>
 *
 * <p>注意：注解（如 {@code @RocketMQMessageListener}）的属性值要求编译期常量，
 * 因此这里全部使用 {@code public static final String}。</p>
 *
 * @author demo
 */
public final class RocketMqConstant {

    private RocketMqConstant() {
        // 工具类，禁止实例化
    }

    /* ==================== NameServer（与 application.yml 保持一致，仅作说明） ==================== */
    /** NameServer 地址，实际连接地址由 application.yml 中 rocketmq.name-server 决定 */
    public static final String NAME_SERVER = "172.16.75.106:9876";

    /* ==================== 生产者组 ==================== */
    /** 生产者组名，与 application.yml 中 rocketmq.producer.group 一致 */
    public static final String PRODUCER_GROUP = "demo-producer-group";

    /* ==================== Topic 定义（按场景划分） ==================== */
    /** 基础 Topic：用于同步 / 异步 / 单向 / 生命周期 等常规演示 */
    public static final String TOPIC_BASIC = "demo-basic-topic";
    /** 延迟消息 Topic */
    public static final String TOPIC_DELAY = "demo-delay-topic";
    /** 顺序消息 Topic */
    public static final String TOPIC_ORDER = "demo-order-topic";
    /** 批量消息 Topic */
    public static final String TOPIC_BATCH = "demo-batch-topic";
    /** 事务消息 Topic */
    public static final String TOPIC_TRANSACTION = "demo-tx-topic";
    /** 带 Tag/Key 的消息 Topic（用于 Tag 过滤消费演示） */
    public static final String TOPIC_TAG = "demo-tag-topic";
    /** 失败重试 Topic */
    public static final String TOPIC_RETRY = "demo-retry-topic";
    /** 广播消费 Topic */
    public static final String TOPIC_BROADCAST = "demo-broadcast-topic";
    /** 可靠消息 Topic：演示本地消息表（幂等去重 + 失败记录 + 数据库重试） */
    public static final String TOPIC_RELIABLE = "demo-reliable-topic";

    /* ==================== 消费者组定义（每个消费者独立） ==================== */
    /** 基础并发消费者组 */
    public static final String GROUP_BASIC = "demo-basic-consumer-group";
    /** 延迟消息消费者组 */
    public static final String GROUP_DELAY = "demo-delay-consumer-group";
    /** 顺序消费者组 */
    public static final String GROUP_ORDER = "demo-order-consumer-group";
    /** 批量消费者组 */
    public static final String GROUP_BATCH = "demo-batch-consumer-group";
    /** 事务消费者组 */
    public static final String GROUP_TRANSACTION = "demo-tx-consumer-group";
    /** Tag 过滤消费者组 */
    public static final String GROUP_TAG = "demo-tag-consumer-group";
    /** 失败重试消费者组 */
    public static final String GROUP_RETRY = "demo-retry-consumer-group";
    /** 广播消费者组 */
    public static final String GROUP_BROADCAST = "demo-broadcast-consumer-group";
    /** 生命周期定制（手动控制）消费者组 */
    public static final String GROUP_LIFECYCLE = "demo-lifecycle-consumer-group";
    /** 可靠消息消费者组 */
    public static final String GROUP_RELIABLE = "demo-reliable-consumer-group";

    /* ==================== Tag 定义 ==================== */
    /** 标签 A：Tag 过滤消费只订阅该标签 */
    public static final String TAG_A = "tagA";
    /** 标签 B：用于对比演示（Tag 过滤消费者默认不会消费到） */
    public static final String TAG_B = "tagB";
}
