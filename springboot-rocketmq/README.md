# Spring Boot 3 + Apache RocketMQ 学习 Demo

一个覆盖 **生产者 / 消费者全部常见场景** 的 RocketMQ 学习示例工程，代码含详细中文注释，每个场景均为独立的类或 HTTP 接口，方便单独触发与测试。

- **生产者场景**：同步、异步（带回调）、单向、延迟、顺序、批量、事务、带 Tag&Key 发送
- **消费者场景**：并发消费、顺序消费、广播消费、Tag 过滤、失败重试、延迟消费、批量消费、事务消费、生命周期定制（位点/ack 控制）

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
    │   │   └── ProducerController.java         # 8+2 个发送场景，每个独立 HTTP 接口
    │   ├── controller/
    │   │   └── ReliableController.java         # 可靠消息演示接口（发送/重复/失败/查询/手动重试）
    │   ├── transaction/
    │   │   └── OrderTransactionListener.java   # 事务消息：本地事务执行 + 状态回查
    │   ├── entity/
    │   │   └── MqConsumeRecord.java            # 本地消息表实体（幂等/失败/重试）
    │   ├── enums/
    │   │   └── ConsumeStatus.java              # 消费状态：CONSUMING/SUCCESS/FAILED/DEAD
    │   ├── repository/
    │   │   └── MqConsumeRecordRepository.java  # 本地消息表 JPA 仓储
    │   ├── reliability/                        # ★ 消息可靠性框架（本地消息表模式）
    │   │   ├── MessageCallback.java            # 业务回调接口
    │   │   ├── ReliableMessageHandler.java     # 重放处理器接口（按 topic 反查）
    │   │   ├── ReliableMessageHandlerRegistry.java # 处理器注册中心
    │   │   ├── MessageReliabilityService.java  # 核心：落库+幂等+失败记录+重试
    │   │   └── FailedMessageRetryScheduler.java# 定时扫描失败消息并重试
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
  datasource:                                   # 本地消息表 mq_consume_record 的存储
    url: jdbc:mysql://172.16.75.105:3306/springboot-rocketmq?...&createDatabaseIfNotExist=true
    username: root
    password: abcd@123456
  jpa:
    hibernate:
      ddl-auto: update                          # 首次启动自动建表

reliability:
  max-retry: 3                                  # 最大重试次数，达到后转死信(DEAD)
  base-retry-interval-seconds: 10               # 递增退避：第 n 次重试延迟 = 10 * n 秒
  retry-scan-interval-millis: 10000             # 失败消息扫描间隔
  retry-batch-size: 50                          # 每轮扫描最多处理条数
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
| ReliableOrderConsumer | 默认 + 集成 `MessageReliabilityService` | ⑩ 消息落库、幂等去重、失败记录、数据库重试（见第八节） |

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

## 八、消息可靠性设计（本地消息表：持久化 / 失败记录 / 数据库重试 / 幂等）

本节是本模块的**重点进阶内容**，演示生产级「消息可靠消费」的完整设计。相比 broker 自带重试（`RetryConsumer`），本方案把消息**落库 MySQL**，重试策略完全可控、过程可查、可人工干预。

### 8.1 设计目标

| 诉求 | 实现方式 |
| --- | --- |
| 消息持久化到 MySQL | 每条消费的消息落库到 `mq_consume_record` 表 |
| 防止重复消费（幂等） | `(biz_key, topic)` 唯一索引 + 状态判断，重复投递自动跳过 |
| 失败记录 | 消费失败保存 `status=FAILED`、`error_msg`、`retry_count`、`next_retry_time` |
| 从数据库重试 | `@Scheduled` 定时扫描到期失败记录，反查处理器**重放**业务 |
| 死信兜底 | 重试达 `max-retry` 仍失败 → `status=DEAD`，停止自动重试 |

### 8.2 状态流转

```
CONSUMING（首次落库）
   ├─ 业务成功 ─────────────────▶ SUCCESS（终态；重复投递凭此幂等跳过）
   └─ 业务失败 ─▶ FAILED（retry_count+1，next_retry_time = now + 10*n 秒）
                     ├─ 定时任务重试成功 ─▶ SUCCESS
                     └─ retry_count ≥ max-retry ─▶ DEAD（死信，人工处理）
```

### 8.3 关键设计：为什么失败时「不抛异常给 broker」

若消费方法抛异常，broker 也会重试，就会与数据库重试形成**双重重试**。因此 `ReliableOrderConsumer.onMessage` 内部**吞掉业务异常并正常返回**（等于向 broker ack），失败改由本地消息表接管重试，保证 broker 侧只投递一次、重试次数与间隔完全由 `reliability.*` 配置决定。

### 8.4 核心组件

| 组件 | 职责 |
| --- | --- |
| `MqConsumeRecord` | 本地消息表实体，含唯一索引与重试字段 |
| `ConsumeStatus` | 状态枚举 CONSUMING/SUCCESS/FAILED/DEAD |
| `MessageReliabilityService` | 核心：幂等落库 → 执行业务 → 记录成功/失败/排期 |
| `ReliableMessageHandler` + `Registry` | 按 topic 注册业务处理器，供重试时反查重放 |
| `FailedMessageRetryScheduler` | `@Scheduled` 定时扫描到期失败记录并重试 |
| `ReliableOrderConsumer` | 集成上述能力的示例消费者 |
| `ReliableController` | 触发各场景 + 查询记录 + 手动重试 |

### 8.5 可靠消息接口与体验顺序

前缀 `http://localhost:11001/rocketmq-demo`，导航页 `/reliable/index`。

| 步骤 | 接口 | 现象 |
| --- | --- | --- |
| 1 | `/reliable/send?orderId=REL-1001&action=NORMAL` | 正常消费，`/reliable/records` 出现 `SUCCESS` |
| 2 | `/reliable/send-duplicate?orderId=REL-DUP-1` | 同 orderId 发两次，第二次日志「幂等跳过」，DB 仅 1 条 |
| 3 | `/reliable/fail-once?orderId=REL-FO-1` | 首次失败落库 `FAILED`，定时任务/`/reliable/retry-now` 后变 `SUCCESS` |
| 4 | `/reliable/fail-always?orderId=REL-FA-1` | 多次 `/reliable/retry-now` 后 `retry_count` 达上限 → `DEAD` |
| — | `/reliable/records` | 查询最近 100 条消费记录（状态/重试次数/错误/下次重试时间） |
| — | `/reliable/record?bizKey=REL-1001` | 按业务键查询单条 |
| — | `/reliable/stats` | 各状态数量统计，观察 FAILED/DEAD 堆积 |
| — | `/reliable/retry-now` | 手动触发一轮数据库重试（等价定时任务立即执行） |

### 8.6 建表 DDL（参考，默认由 `ddl-auto=update` 自动创建）

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
  UNIQUE KEY `uk_biz_topic` (`biz_key`, `topic`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 8.7 生产环境注意事项

- **幂等键**：务必让生产者用 `RocketMQHeaders.KEYS` 设置业务唯一键（本 Demo 用 orderId），否则退化为 msgId 无法跨重投去重。
- **多实例并发重试**：调度器在多实例下会并发扫描，需加分布式锁或用「乐观抢占」（`update ... set status=RETRYING where id=? and status=FAILED`）避免重复重试。本 Demo 为单实例，未加锁。
- **业务幂等**：数据库重试会重复调用业务逻辑，业务本身仍需保证幂等。
- **表膨胀**：`mq_consume_record` 会持续增长，生产环境应定期归档/清理终态（SUCCESS/DEAD）记录。

---

## 九、在 RocketMQ Dashboard 观察（http://172.16.75.106:8080）

- **主题（Topic）**：查看自动创建的 `demo-*-topic`；点「状态」看各队列消息总量、最后更新时间。
- **消费者（Consumer）**：查看各消费组的在线状态、**消费进度（Diff/延迟）**、TPS；`Diff=0` 表示无堆积。
- **消息（Message）**：
  - 按 **Topic + 时间范围** 查询；
  - 按 **Message Key** 精确查询（代码中通过 `RocketMQHeaders.KEYS` 设置，如 `TAGKEY-xxx`）；
  - 点「Message Track」查看消息**投递给了哪些消费组、是否消费成功**。
- **事务消息**：提交前半消息存于 `RMQ_SYS_TRANS_HALF_TOPIC`，提交后才对消费者可见。
- **重试/死信**：可在 Topic 列表看到 `%RETRY%demo-retry-consumer-group`、`%DLQ%demo-retry-consumer-group`。

---

## 十、RocketMQ 延迟级别对照表

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

## 十一、常见问题（FAQ）

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
A：① 确认启动类已加 `@EnableScheduling`；② 检查 `next_retry_time` 是否已到（默认首次 10s 后）；③ 可手动调 `/reliable/retry-now` 立即触发；④ 查 `/reliable/stats` 看是否已达 `max-retry` 转 DEAD。

---

## 十二、与父工程的关系

本模块是 `springboot-collection-jdk21` 的子模块，继承公共依赖（lombok 等）与插件管理，但：

- 通过覆盖 `boot-version` 属性 + 自身 `dependencyManagement` **独立锁定 Spring Boot 3.3.5**；
- 使用标准 `spring-boot-starter-web`（非内部 `springboot-starter-web`），**便于单独拷贝运行**；
- 额外引入 `spring-boot-starter-data-jpa` + `mysql-connector-j`，用于本地消息表持久化。
