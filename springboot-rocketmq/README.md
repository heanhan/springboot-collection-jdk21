# Spring Boot 3 + Apache RocketMQ 学习 Demo

一个覆盖 **生产者 / 消费者全部常见场景** 的 RocketMQ 学习示例工程，代码含详细中文注释，每个场景均为独立的类或 HTTP 接口，方便单独触发与测试。

- **生产者场景**：同步、异步（带回调）、单向、延迟、顺序、批量、事务、带 Tag&Key 发送
- **消费者场景**：并发消费、顺序消费、广播消费、Tag 过滤、失败重试、延迟消费、批量消费、事务消费、生命周期定制（位点/ack 控制）
- **消息可靠性落库（三表分离）**：生产端发送前落库 `mq_produce_record`（PENDING→SUCCESS/FAILED），消费端接收落库 `mq_consume_record`（CONSUMING→SUCCESS/FAILED/DEAD，按消费组幂等）；**生产失败与消费失败连同原始报文统一落入重试表 `mq_message_retry`**（用 `retry_type` 区分 PRODUCE/CONSUME），由定时调度器「取原始数据」重新生产 / 重新消费
- **JUC 异步落库**：发送/消费**成功**的状态回写走独立线程池异步执行（有界队列 + `CallerRunsPolicy` 背压 + 优雅停机），主链路不阻塞；**失败入重试表**等关键路径同步执行，绝不丢

---

## 一、技术栈与版本选型（关键决策，务必了解）

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | Oracle OpenJDK 21 |
| Spring Boot | **3.3.5** | 本模块覆盖父工程默认的 3.2.0 |
| rocketmq-spring-boot-starter | **2.3.1** | Spring Boot 3 是 jakarta 命名空间，**必须使用 2.3.x**；2.2.x 面向 Boot 2（javax），不能用 |
| rocketmq-client（starter 传递） | 5.3.0 | remoting 协议，**向下兼容 RocketMQ 4.9.x broker** |
| RocketMQ broker（服务端） | 4.9.6 | 实际部署的消息服务器 |
| NameServer | `172.16.75.106:9876` | 未开启 ACL |
| RocketMQ Dashboard | `http://172.16.75.106:8080` | 可视化观察消息与消费情况 |

> **为什么客户端不锁定 4.9.6？**
> starter 2.3.1 是针对 rocketmq-client 5.x 编译的，强行把 client 降级到 4.9.6 可能出现 `NoSuchMethodError`。
> 官方支持 5.x remoting 客户端连接 4.9.x broker，这是最稳妥的组合。

---

## 二、环境准备

1. **启动 RocketMQ 4.9.6**（NameServer + Broker），确保监听在 `172.16.75.106:9876`。
2. **开启自动创建 Topic**：在 broker 的 `broker.conf` 中设置
   ```properties
   autoCreateTopicEnable=true
   ```
   否则首次发送会报 `Topic not exist`。生产环境建议关闭自动创建、手动建 Topic。
3. **启动 RocketMQ Dashboard**（可选）：`http://172.16.75.106:8080`，并在其配置中指向同一 NameServer。

---

## 三、项目目录结构

```bash
springboot-rocketmq/
├── pom.xml                                     # Boot 3.3.5 + rocketmq-starter 2.3.1
└── src/main/
    ├── java/org/example/rocketmq/
    │   ├── RocketMqApplication.java            # 启动类（含访问入口提示）
    │   ├── common/
    │   │   ├── RocketMqConstant.java           # 统一常量：Topic / Group / Tag
    │   │   └── OrderMessage.java               # 统一消息体（订单示例）
    │   ├── producer/
    │   │   └── ProducerController.java         # 8+2 个发送场景，每个独立 HTTP 接口（发送前落库、成功异步回写、失败入重试表）
    │   ├── controller/
    │   │   ├── ReliableController.java         # 可靠消息演示接口（发送/重复/失败/查询/手动重试）
    │   │   └── MessageRecordController.java    # ★ 生产记录 + 统一重试表查询接口
    │   ├── transaction/
    │   │   └── OrderTransactionListener.java   # 事务消息：本地事务执行 + 状态回查
    │   ├── concurrent/
    │   │   └── AsyncRecordExecutor.java        # ★ JUC 异步落库线程池（成功状态回写异步化）
    │   ├── entity/
    │   │   ├── MqProduceRecord.java            # ★ 生产端消息表（仅生产状态）
    │   │   ├── MqConsumeRecord.java            # 消费端消息表（幂等/消费状态，按组唯一）
    │   │   └── MqMessageRetry.java             # ★ 统一重试表（retry_type 区分生产/消费失败 + 原始报文）
    │   ├── enums/
    │   │   ├── ProduceStatus.java              # 生产状态：PENDING/SUCCESS/FAILED
    │   │   ├── ConsumeStatus.java              # 消费状态：CONSUMING/SUCCESS/FAILED/DEAD
    │   │   ├── RetryType.java                  # ★ 重试类型：PRODUCE/CONSUME
    │   │   └── RetryStatus.java                # ★ 重试状态：PENDING/RETRYING/SUCCESS/DEAD
    │   ├── repository/
    │   │   ├── MqProduceRecordRepository.java  # 生产消息表 JPA 仓储
    │   │   ├── MqConsumeRecordRepository.java  # 消费消息表 JPA 仓储
    │   │   └── MqMessageRetryRepository.java   # ★ 统一重试表 JPA 仓储（含乐观抢占 compareAndSetStatus）
    │   ├── reliability/                        # ★ 消息可靠性框架（三表分离 + 统一重试）
    │   │   ├── MessageCallback.java            # 业务回调接口
    │   │   ├── ReliableMessageHandler.java     # 重放处理器接口（按 topic 反查）
    │   │   ├── ReliableMessageHandlerRegistry.java # 处理器注册中心
    │   │   ├── MessageRecordService.java       # ★ 生产端：发送前落库 + 成功异步回写 + 失败同步入重试表
    │   │   ├── MessageReliabilityService.java  # ★ 消费端：接收落库 + 幂等 + 成功异步回写 + 失败同步入重试表
    │   │   ├── MessageRetryService.java        # ★ 统一重试：落重试表 + 生产重发 + 消费重放
    │   │   └── MessageRetryScheduler.java      # ★ 定时扫描 mq_message_retry（乐观抢占）并重试
    │   └── consumer/
    │       ├── BasicConcurrentConsumer.java    # ① 并发消费（Push + 集群）
    │       ├── OrderlyConsumer.java            # ② 顺序消费（ORDERLY）
    │       ├── BroadcastConsumer.java          # ③ 广播消费（BROADCASTING）
    │       ├── TagFilterConsumer.java          # ④ Tag 过滤消费
    │       ├── RetryConsumer.java              # ⑤ 失败重试 + 异常处理（broker 重试）
    │       ├── DelayConsumer.java              # ⑥ 延迟消息消费
    │       ├── BatchConsumer.java              # ⑦ 批量消息消费
    │       ├── TransactionConsumer.java        # ⑧ 事务消息消费
    │       ├── LifecycleManualAckConsumer.java # ⑨ 生命周期定制 / 位点(ack)控制
    │       └── ReliableOrderConsumer.java      # ⑩ 可靠消费（本地消息表+幂等+DB重试）
    └── resources/
        └── application.yml                     # 完整配置（含 datasource / jpa / reliability）
```

---

## 四、配置说明（application.yml）

```yaml
server:
  port: 11001
  servlet:
    context-path: /rocketmq-demo   # 所有接口统一前缀

rocketmq:
  name-server: 172.16.75.106:9876  # NameServer 地址，集群用分号(;)分隔
  producer:
    group: demo-producer-group     # 【必填】生产者组，事务消息也复用它
    send-message-timeout: 3000     # 同步发送超时(ms)
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2
    max-message-size: 4194304      # 单条消息最大 4MB
```

> **说明**：消费者的 Topic、消费组、消费模式（集群/广播）、Tag 过滤等，均在各自 `@RocketMQMessageListener` 注解中声明，**不在 yml 里全局配置**，以保证每个消费者示例互相独立、可单独运行。

### 数据源与可靠消息配置（本地消息表）

```yaml
spring:
  datasource:                                   # 三张表（mq_produce_record / mq_consume_record / mq_message_retry）的存储
    url: jdbc:mysql://172.16.75.105:3306/springboot-rocketmq?...&createDatabaseIfNotExist=true
    username: root
    password: abcd@123456
  jpa:
    hibernate:
      ddl-auto: update                          # 首次启动自动建表

reliability:
  max-retry: 3                                  # 最大重试次数，达到后转死信(DEAD)
  base-retry-interval-seconds: 10               # 递增退避：第 n 次重试延迟 = 10 * n 秒
  retry-scan-interval-millis: 10000             # 统一重试表扫描间隔
  retry-batch-size: 50                          # 每轮扫描最多处理条数
  async:                                        # ★ JUC 异步落库线程池（成功状态回写异步化）
    core-pool-size: 4                           # 核心线程数
    max-pool-size: 8                            # 最大线程数
    queue-capacity: 2000                        # 有界队列容量（防 OOM，满时 CallerRunsPolicy 背压）
    keep-alive-seconds: 60                       # 非核心线程空闲存活时间
```

> **启动前置条件**：本模块已引入 MySQL（本地消息表），启动前需确保 `172.16.75.105:3306` 可连；库 `springboot-rocketmq` 会由 `createDatabaseIfNotExist=true` 自动创建，表由 `ddl-auto=update` 自动生成。

所有 Topic / Group / Tag 常量集中在 [`RocketMqConstant.java`](src/main/java/org/example/rocketmq/common/RocketMqConstant.java)，方便统一修改：

| 常量 | 值 |
| --- | --- |
| TOPIC_BASIC | `demo-basic-topic` |
| TOPIC_DELAY | `demo-delay-topic` |
| TOPIC_ORDER | `demo-order-topic` |
| TOPIC_BATCH | `demo-batch-topic` |
| TOPIC_TRANSACTION | `demo-tx-topic` |
| TOPIC_TAG | `demo-tag-topic` |
| TOPIC_RETRY | `demo-retry-topic` |
| TOPIC_BROADCAST | `demo-broadcast-topic` |
| TOPIC_RELIABLE | `demo-reliable-topic` |
| TAG_A / TAG_B | `tagA` / `tagB` |

---

## 五、快速开始

### 方式一：IDE 启动
直接运行 `RocketMqApplication` 的 `main` 方法。

### 方式二：命令行启动
```bash
cd springboot-rocketmq
mvn spring-boot:run
```

### 方式三：打包后运行
```bash
mvn -pl springboot-rocketmq -am clean package -DskipTests
java -jar springboot-rocketmq/target/springboot-rocketmq.jar
```

启动成功后，控制台会打印访问入口。打开**测试导航页**（列出全部接口，可直接点击）：

```
http://localhost:11001/rocketmq-demo/producer/index
```

---

## 六、生产者接口清单与推荐测试顺序

所有接口均为 `GET`，前缀 `http://localhost:11001/rocketmq-demo`。触发后请**同时观察控制台对应消费者的日志**。

| 顺序 | 接口 | 场景 | 预期现象 |
| --- | --- | --- | --- |
| 1 | `/producer/sync` | 同步发送 | `BasicConcurrentConsumer` 与 `LifecycleManualAckConsumer` 各消费一次（同 Topic 不同组） |
| 2 | `/producer/async` | 异步发送（回调） | 控制台打印 `onSuccess` 回调日志 |
| 3 | `/producer/oneway` | 单向发送 | 无返回结果，仅消费者日志 |
| 4 | `/producer/delay?delayLevel=3` | 延迟发送 | 约 **10 秒后** `DelayConsumer` 才收到，日志显示实际延迟时长 |
| 5 | `/producer/order?orderId=ORDER-1001` | 顺序发送 | `OrderlyConsumer` 严格按 CREATE→PAY→DELIVER→FINISH 顺序打印 |
| 6 | `/producer/batch?count=10` | 批量发送 | `BatchConsumer` 逐条打印 10 条 |
| 7 | `/producer/tagkey` | 带 Tag&Key 发送 | `TagFilterConsumer` 只收到 tagA，tagB 被 broker 过滤 |
| 8 | `/producer/transaction` | 事务发送 | 先执行本地事务→提交，`TransactionConsumer` 随后才收到 |
| 9 | `/producer/retry` | 触发失败重试 | `RetryConsumer` 重试次数递增，达阈值后转人工处理 |
| 10 | `/producer/broadcast` | 触发广播消费 | `BroadcastConsumer` 收到；多实例启动时每个实例都收到 |

> 参数均可省略，省略时使用默认值。例如 `/producer/order` 默认 `orderId=ORDER-1001`。

---

## 七、消费者场景要点

| 消费者 | 关键注解配置 | 说明 |
| --- | --- | --- |
| BasicConcurrentConsumer | 默认（CONCURRENTLY + CLUSTERING） | 最常用；组内一条消息只消费一次 |
| OrderlyConsumer | `consumeMode = ORDERLY` | 同队列串行消费，保证顺序 |
| BroadcastConsumer | `messageModel = BROADCASTING` | 组内每个实例都消费全量；失败不重试 |
| TagFilterConsumer | `selectorType = TAG` + `selectorExpression = "tagA"` | broker 端按 Tag 过滤 |
| RetryConsumer | 泛型用 `MessageExt` | 抛异常触发重试，可读 `getReconsumeTimes()` |
| DelayConsumer | 默认 | 对比发送/消费时刻观察延迟 |
| BatchConsumer | 默认 | 批量仅是生产端优化，消费端仍逐条回调 |
| TransactionConsumer | 默认 | 只接收已提交的事务消息 |
| LifecycleManualAckConsumer | 实现 `RocketMQPushConsumerLifecycleListener` | 定制线程数/重试次数，演示位点控制 |
| ReliableOrderConsumer | 默认 + 集成 `MessageReliabilityService` | ⑩ 消息落库、幂等去重、失败入重试表、数据库重试（见第九节） |

### 关于「手动确认 / 提交位点」

starter 的 Push 消费模型采用**自动位点管理**：

- `onMessage` **正常返回** = 消费成功，客户端后台**自动提交位点（ack）**；
- `onMessage` **抛出异常** = 消费失败，等价于 `RECONSUME_LATER`，**触发重试**。

因此在注解式 Push 模型中通常**无需也无法**逐条手动 commit offset。若确需完全手动控制位点，应改用原生 `DefaultLitePullConsumer`（手动 `poll()` + `commitSync()`）。详见 `LifecycleManualAckConsumer` 的注释。

### 重试与死信

- 消费失败的消息进入重试队列 `%RETRY%消费组`，按递增延迟（10s、30s、1m、2m…）重投，**默认最多 16 次**；
- 16 次仍失败进入死信队列 `%DLQ%消费组`，需人工处理；
- **消费端务必做幂等**，因为重试会导致消息被多次投递。

---

## 八、生产端可靠发送（mq_produce_record：发送前落库 + 成功异步回写 + 失败入重试表）

本节对应需求①：**消息生产入库——发送前落库；发送成功后更新生产状态；发送失败落入独立重试表（保存原始报文），供后续「直接取原始数据进行生产重试请求（重新发送）」**。

### 8.1 总体架构（三表分离 + 统一重试）

| 表 | 职责 | 关键字段 |
| --- | --- | --- |
| `mq_produce_record` | **生产端**：一条被生产的消息的发送生命周期 | `produce_status`(PENDING/SUCCESS/FAILED)、`body`(原始报文) |
| `mq_consume_record` | **消费端**：一次消费的幂等与状态（见第九节） | `status`(CONSUMING/SUCCESS/FAILED/DEAD)、`consumer_group` |
| `mq_message_retry` | **统一重试**：生产失败 + 消费失败 | `retry_type`(PRODUCE/CONSUME)、`status`(PENDING/RETRYING/SUCCESS/DEAD)、`body`、`next_retry_time` |

三表通过 `(bizKey, topic)` 关联；重试表用 `source_record_id` 回指来源业务记录，便于回溯。

> **为什么重试独立成表**：① 与业务记录表解耦，重试调度只扫描这张「小表」，索引 `(retry_type,status,next_retry_time)` 命中快；② 用 `retry_type` 一张表**区分「生产者失败消息」与「消费者失败消息」**，统一退避与死信处理；③ 保存原始报文，重试无需回查业务表，可直接重放。

### 8.2 同步 / 异步边界（不影响性能，JUC 多线程）

| 操作 | 方式 | 原因 |
| --- | --- | --- |
| 发送前落库 PENDING | **同步** | 先落库再发送，宕机也有据可查 |
| 发送成功回写 SUCCESS | **异步**（`AsyncRecordExecutor` JUC 线程池） | 主发送链路不等待 DB 写，提升吞吐；偶发丢失可由补偿修正 |
| 发送失败置 FAILED + 入重试表 | **同步** | 关乎可靠性，绝不能丢 |

`AsyncRecordExecutor` 采用 `ThreadPoolExecutor` + **有界队列** `ArrayBlockingQueue`（防 OOM）+ **`CallerRunsPolicy`**（队列满时由调用线程执行，形成天然背压）+ 命名守护线程工厂 + `@PreDestroy` **优雅停机**（等待在途任务完成）。

### 8.3 生产端状态流转

```
PENDING（发送前同步落库）
   ├─ broker SEND_OK ─▶ SUCCESS（异步回填 msgId、produce_time）
   └─ 发送异常 ──────▶ FAILED（同步；同时写 mq_message_retry[type=PRODUCE]，保存 body）
                          └─ 调度器取 body 用原生 Producer 重新发送（保留 KEYS=bizKey）
                                ├─ 重发成功 ─▶ 重试记录 SUCCESS + 回写生产记录 SUCCESS
                                └─ 达 max-retry 仍失败 ─▶ 重试记录 DEAD（人工处理）
```

### 8.4 事务消息的状态处理

`/producer/transaction` 发送后，按 `TransactionSendResult.getLocalTransactionState()` 判定生产记录状态：
`COMMIT`→置 SUCCESS；`ROLLBACK`→置 FAILED 并入重试表（半消息被丢弃）；`UNKNOWN`→保持 PENDING，等待 broker 回查 / 补偿任务最终确定。

### 8.5 核心组件

| 组件 | 职责 |
| --- | --- |
| `MessageRecordService` | 生产端核心：`createPending`(同步) / `markProduceSuccess`(异步) / `markProduceFailed`(同步 + 入重试表) |
| `AsyncRecordExecutor` | JUC 异步落库线程池，承载「成功状态回写」 |
| `MqProduceRecord` / `MqProduceRecordRepository` | 生产表实体与仓储（仅生产状态，`(bizKey,topic)` 唯一） |
| `MessageRecordController` | 生产记录 + 统一重试表查询接口 |

### 8.6 查询接口

前缀 `http://localhost:11001/rocketmq-demo`，导航页 `/message-record/index`。

| 接口 | 现象 |
| --- | --- |
| `/message-record/records` | 最近 100 条生产记录 |
| `/message-record/records-by-topic?topic=demo-basic-topic` | 按 Topic 查询 |
| `/message-record/produce-stats` | 生产状态统计（观察 PENDING/FAILED 堆积） |
| `/message-record/retry-records?type=PRODUCE` | 生产失败重试记录 |
| `/message-record/retry-stats` | 重试表按「类型 + 状态」统计 |
| `/message-record/stats` | 生产 + 重试状态总览 |

### 8.7 建表 DDL（参考，默认由 `ddl-auto=update` 自动创建）

```sql
CREATE TABLE `mq_produce_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_key`          VARCHAR(128) NOT NULL COMMENT '业务幂等键（消息 Key）',
  `topic`            VARCHAR(128) NOT NULL COMMENT '消息主题',
  `tags`             VARCHAR(64)           COMMENT '消息标签',
  `body`             TEXT                  COMMENT '消息体(JSON)，生产重试时直接取此重发',
  `producer_group`   VARCHAR(128)          COMMENT '生产者组',
  `msg_id`           VARCHAR(128)          COMMENT 'RocketMQ msgId（发送成功后回填）',
  `produce_status`   VARCHAR(16)  NOT NULL COMMENT 'PENDING/SUCCESS/FAILED',
  `produce_time`     DATETIME              COMMENT '发送完成时间',
  `produce_error`    VARCHAR(1000)         COMMENT '发送失败原因',
  `created_time`     DATETIME,
  `updated_time`     DATETIME,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_topic` (`biz_key`, `topic`),
  KEY `idx_produce_status` (`produce_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.8 生产环境注意事项

- **幂等键**：生产者务必用 `RocketMQHeaders.KEYS` 设置业务唯一键；生产重试重发时会原样带回 `KEYS=bizKey`，保证消费端幂等键一致。
- **补偿重发**：可新增定时任务扫描 `produce_status=PENDING` 且 `created_time` 过久的记录重新发送，应对「落库后、发送前宕机」。
- **表膨胀**：`mq_produce_record` / `mq_message_retry` 会持续增长，生产环境应定期归档/清理终态记录。

---

## 九、消费端可靠消费（mq_consume_record：接收落库 + 幂等 + 失败入重试表）与统一重试调度

本节对应需求②③：**消费时接收消息即落库；消费成功/失败更新消费状态；消费失败连同原始报文录入统一重试表(type=CONSUME)，供后续「取原始数据进行消费重试」**。

### 9.1 设计目标

| 诉求 | 实现方式 |
| --- | --- |
| 消息持久化到 MySQL | 每条消费的消息**接收即落库** `mq_consume_record`(CONSUMING) |
| 防止重复消费（幂等） | `(biz_key, topic, consumer_group)` 唯一索引 + 状态判断，本组重复投递自动跳过 |
| 消费成功/失败更新状态 | 成功→**异步**回写 SUCCESS；失败→**同步**置 FAILED |
| 消费失败入重试表 | 失败时把原始报文写入 `mq_message_retry`(type=CONSUME) |
| 从数据库重试 | `MessageRetryScheduler` 扫描到期记录，按 topic 反查处理器**重放**业务 |
| 死信兜底 | 重试达 `max-retry` 仍失败 → 重试记录 DEAD、消费记录 DEAD |

### 9.2 状态流转

```
消费记录(mq_consume_record)：
CONSUMING（接收即同步落库）
   ├─ 业务成功 ─▶ SUCCESS（异步回写；重复投递凭此幂等跳过）
   └─ 业务失败 ─▶ FAILED（同步）+ 写 mq_message_retry[type=CONSUME]
                     ├─ 调度器重放成功 ─▶ 消费记录回写 SUCCESS
                     └─ 达 max-retry ─▶ 消费记录 DEAD

重试记录(mq_message_retry)：
PENDING ─(调度器乐观抢占)─▶ RETRYING ─成功─▶ SUCCESS
                                   └─失败─▶ retry_count+1；未达上限回 PENDING（递增退避），达上限 DEAD
```

### 9.3 关键设计

- **失败不抛异常给 broker**：`consume()` 内部吞掉业务异常（等于 ack），改由数据库重试接管，避免 broker 重试与数据库重试双重触发。（broker 原生重试演示见 `RetryConsumer`，它只记录状态、不入重试表。）
- **同步/异步边界**：接收落库（同步，幂等前置）、失败入重试表（同步）、成功回写（异步，JUC 线程池）。
- **消费重放需注册处理器**：`MessageRetryService.retryConsume` 按 topic 反查 `ReliableMessageHandler`；`ReliableOrderConsumer` 已实现该接口(TOPIC_RELIABLE)，故其失败可端到端自动重放。其它 topic 若无处理器，重试记录保持 PENDING 并告警（生产环境应为该 topic 注册处理器）。
- **多消费组幂等**：唯一键含 `consumer_group`，故 `TOPIC_BASIC` 被两个组消费时各自独立落库、互不冲突。

### 9.4 核心组件

| 组件 | 职责 |
| --- | --- |
| `MessageReliabilityService` | 消费端核心：`consume()` 幂等落库→业务→成功异步/失败同步入重试表；`recordConsuming/Success/Failed/Dead` 轻量记录（供 broker 重试演示） |
| `MessageRetryService` | 统一重试：`saveProduceRetry/saveConsumeRetry` 落表；`executeRetry` 按类型分发；`retryProduce` 原生重发 / `retryConsume` 反查处理器重放；递增退避 + 死信 |
| `MessageRetryScheduler` | `@Scheduled` 扫描 `mq_message_retry` 到期 PENDING，乐观抢占(`compareAndSetStatus` PENDING→RETRYING)后执行重试；多实例并发安全 |
| `ReliableMessageHandler` + `Registry` | 按 topic 注册业务处理器，供消费重试反查重放 |
| `ReliableOrderConsumer` / `ReliableController` | 端到端可靠消费示例 + 触发/查询/手动重试接口 |

### 9.5 可靠消息接口与体验顺序

前缀 `http://localhost:11001/rocketmq-demo`，导航页 `/reliable/index`。

| 步骤 | 接口 | 现象 |
| --- | --- | --- |
| 1 | `/reliable/send?orderId=REL-1001&action=NORMAL` | 正常消费，`/reliable/records` 出现 `SUCCESS` |
| 2 | `/reliable/send-duplicate?orderId=REL-DUP-1` | 同 orderId 发两次，第二次日志「幂等跳过」，DB 仅 1 条 |
| 3 | `/reliable/fail-once?orderId=REL-FO-1` | 首次失败落库 `FAILED` + 入重试表；定时任务/`/reliable/retry-now` 重放后变 `SUCCESS` |
| 4 | `/reliable/fail-always?orderId=REL-FA-1` | 多次 `/reliable/retry-now` 后重试记录 `retry_count` 达上限 → `DEAD` |
| — | `/reliable/records` | 查询最近 100 条消费记录（状态/重试次数/错误） |
| — | `/reliable/record?bizKey=REL-1001` | 按业务键查询单条 |
| — | `/reliable/stats` | 各状态数量统计，观察 FAILED/DEAD 堆积 |
| — | `/reliable/retry-now` | 手动触发一轮重试（扫 `mq_message_retry`，等价定时任务立即执行） |

### 9.6 建表 DDL（参考，默认由 `ddl-auto=update` 自动创建）

```sql
CREATE TABLE `mq_consume_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_key`         VARCHAR(128) NOT NULL COMMENT '业务幂等键（消息 Key）',
  `topic`           VARCHAR(128) NOT NULL COMMENT '消息主题',
  `msg_id`          VARCHAR(128)          COMMENT 'RocketMQ msgId',
  `tags`            VARCHAR(64)           COMMENT '消息标签',
  `consumer_group`  VARCHAR(128)          COMMENT '消费组',
  `body`            TEXT                  COMMENT '消息体(JSON)',
  `status`          VARCHAR(16)  NOT NULL COMMENT 'CONSUMING/SUCCESS/FAILED/DEAD',
  `retry_count`     INT          NOT NULL DEFAULT 0,
  `max_retry`       INT          NOT NULL DEFAULT 3,
  `next_retry_time` DATETIME,
  `error_msg`       VARCHAR(1000),
  `created_time`    DATETIME,
  `updated_time`    DATETIME,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_topic_group` (`biz_key`, `topic`, `consumer_group`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `mq_message_retry` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `retry_type`       VARCHAR(16)  NOT NULL COMMENT 'PRODUCE/CONSUME',
  `biz_key`          VARCHAR(128) NOT NULL COMMENT '业务幂等键',
  `topic`            VARCHAR(128) NOT NULL COMMENT '消息主题',
  `tags`             VARCHAR(64)           COMMENT '消息标签',
  `msg_id`           VARCHAR(128)          COMMENT 'RocketMQ msgId（消费重试时有值）',
  `group_name`       VARCHAR(128)          COMMENT '生产者组或消费者组',
  `body`             TEXT                  COMMENT '原始消息体(JSON)，重试直接取此重放',
  `status`           VARCHAR(16)  NOT NULL COMMENT 'PENDING/RETRYING/SUCCESS/DEAD',
  `retry_count`      INT          NOT NULL DEFAULT 0,
  `max_retry`        INT          NOT NULL DEFAULT 3,
  `next_retry_time`  DATETIME              COMMENT '下次重试时间',
  `error_msg`        VARCHAR(1000)         COMMENT '最后一次失败原因',
  `source_record_id` BIGINT                COMMENT '来源记录ID（mq_produce_record/mq_consume_record 主键）',
  `created_time`     DATETIME,
  `updated_time`     DATETIME,
  PRIMARY KEY (`id`),
  KEY `idx_type_status_retry` (`retry_type`, `status`, `next_retry_time`),
  KEY `idx_biz_topic` (`biz_key`, `topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 9.7 生产环境注意事项

- **幂等键**：务必让生产者用 `RocketMQHeaders.KEYS` 设置业务唯一键（本 Demo 用 orderId），否则退化为 msgId 无法跨重投去重。
- **多实例并发重试**：调度器已用「乐观抢占」`compareAndSetStatus(PENDING→RETRYING)`，只有抢占成功的实例才重试，天然支持集群部署，无需额外分布式锁。
- **业务幂等**：重试会重复调用业务逻辑，业务本身仍需保证幂等。
- **索引迁移（重要）**：`mq_consume_record` 唯一键由 `(bizKey,topic)` 升级为 `(bizKey,topic,consumerGroup)`。`ddl-auto=update` 只会**新增** `uk_biz_topic_group`，**不会自动删除**旧的 `uk_biz_topic`；若数据库已存在旧唯一索引，需手动执行 `ALTER TABLE mq_consume_record DROP INDEX uk_biz_topic;`，否则多消费组场景下第二个组的落库会因旧唯一键冲突而失败。
- **表膨胀**：`mq_consume_record` / `mq_message_retry` 会持续增长，生产环境应定期归档/清理终态（SUCCESS/DEAD）记录。

---

## 十、在 RocketMQ Dashboard 观察（http://172.16.75.106:8080）

- **主题（Topic）**：查看自动创建的 `demo-*-topic`；点「状态」看各队列消息总量、最后更新时间。
- **消费者（Consumer）**：查看各消费组的在线状态、**消费进度（Diff/延迟）**、TPS；`Diff=0` 表示无堆积。
- **消息（Message）**：
  - 按 **Topic + 时间范围** 查询；
  - 按 **Message Key** 精确查询（代码中通过 `RocketMQHeaders.KEYS` 设置，如 `TAGKEY-xxx`）；
  - 点「Message Track」查看消息**投递给了哪些消费组、是否消费成功**。
- **事务消息**：提交前半消息存于 `RMQ_SYS_TRANS_HALF_TOPIC`，提交后才对消费者可见。
- **重试/死信**：可在 Topic 列表看到 `%RETRY%demo-retry-consumer-group`、`%DLQ%demo-retry-consumer-group`。

---

## 十一、RocketMQ 延迟级别对照表

RocketMQ 4.x **不支持任意时间延迟**，只支持 18 个固定级别（`delayLevel` 从 1 开始）：

| level | 延迟 | level | 延迟 | level | 延迟 |
| --- | --- | --- | --- | --- | --- |
| 1 | 1s | 7 | 3m | 13 | 9m |
| 2 | 5s | 8 | 4m | 14 | 10m |
| 3 | 10s | 9 | 5m | 15 | 20m |
| 4 | 30s | 10 | 6m | 16 | 30m |
| 5 | 1m | 11 | 7m | 17 | 1h |
| 6 | 2m | 12 | 8m | 18 | 2h |

本 Demo 的 `/producer/delay` 默认使用 `delayLevel=3`（10 秒）。

---

## 十二、常见问题（FAQ）

**Q1：启动报 `No route info of this topic`？**
A：Topic 不存在且 broker 未开启自动创建。请在 `broker.conf` 设置 `autoCreateTopicEnable=true` 并重启 broker，或手动创建 Topic。

**Q2：连接不上 NameServer / 发送超时？**
A：检查 `172.16.75.106:9876` 是否可达（`telnet`），以及 broker 是否已向该 NameServer 注册。

**Q3：消费者收不到消息？**
A：① 确认消费组名未被其他不同逻辑的应用占用；② 集群模式下若多个实例同组，消息只会被其中一个消费；③ Tag 过滤消费者只收订阅的 Tag。

**Q4：事务消息消费者一直收不到？**
A：说明本地事务未返回 `COMMIT`（返回了 `UNKNOWN`/`ROLLBACK`）。查看 `OrderTransactionListener` 日志与回查逻辑。

**Q5：能改成 Spring Boot 3.4.x 吗？**
A：可以。修改 `pom.xml` 中的 `<boot-version>` 即可；starter 2.3.1 兼容 Boot 3.x。

**Q6：启动报数据库连接失败 / `Communications link failure`？**
A：本模块引入了本地消息表（MySQL）。请确认 `172.16.75.105:3306` 可达、账号密码正确；库 `springboot-rocketmq` 会由 URL 中 `createDatabaseIfNotExist=true` 自动创建。若只想跑纯 MQ 示例，可移除 `datasource`/`jpa` 配置及 `reliability` 相关包与 JPA/MySQL 依赖。

**Q7：可靠消息一直是 FAILED、不自动重试？**
A：① 确认启动类已加 `@EnableScheduling`；② 检查 `mq_message_retry` 的 `next_retry_time` 是否已到（默认首次 10s 后）；③ 可手动调 `/reliable/retry-now` 立即触发；④ 查 `/reliable/stats` 或 `/message-record/retry-stats` 看是否已达 `max-retry` 转 DEAD。

**Q8：生产记录已 SUCCESS，但 `mq_consume_record` 查不到对应消费记录？**
A：说明消息未被本应用的消费组落库。常见原因：① 生产者未通过 `RocketMQHeaders.KEYS` 设置业务 Key，消费端 `bizKey` 退化为 msgId，两表对不上；② 消息被其它应用/消费组消费，本应用消费组未订阅该 Topic；③ 消费记录按 `(bizKey, topic, consumerGroup)` 唯一，查询时需带上消费组（见 `/reliable/record` 与 9.3 多消费组幂等）。

**Q9：`mq_produce_record`、`mq_consume_record`、`mq_message_retry` 三张表如何分工？**
A：① `mq_produce_record`——生产端，记录一条被生产消息的发送生命周期（PENDING/SUCCESS/FAILED），仅含生产状态（第八节）；② `mq_consume_record`——消费端，记录一次消费的幂等与状态（CONSUMING/SUCCESS/FAILED/DEAD），按 `(bizKey, topic, consumerGroup)` 唯一（第九节）；③ `mq_message_retry`——统一重试表，用 `retry_type` 区分 PRODUCE(生产失败重发)/CONSUME(消费失败重放)，保存原始报文，由 `MessageRetryScheduler` 扫描调度。三者通过 `(bizKey, topic)` 关联，重试表用 `source_record_id` 回指来源记录。

---

## 十三、与父工程的关系

本模块是 `springboot-collection-jdk21` 的子模块，继承公共依赖（lombok 等）与插件管理，但：

- 通过覆盖 `boot-version` 属性 + 自身 `dependencyManagement` **独立锁定 Spring Boot 3.3.5**；
- 使用标准 `spring-boot-starter-web`（非内部 `springboot-starter-web`），**便于单独拷贝运行**；
- 额外引入 `spring-boot-starter-data-jpa` + `mysql-connector-j`，用于本地消息表持久化。
