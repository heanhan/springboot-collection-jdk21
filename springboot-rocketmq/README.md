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
    │   ├── transaction/
    │   │   └── OrderTransactionListener.java   # 事务消息：本地事务执行 + 状态回查
    │   └── consumer/
    │       ├── BasicConcurrentConsumer.java    # ① 并发消费（Push + 集群）
    │       ├── OrderlyConsumer.java            # ② 顺序消费（ORDERLY）
    │       ├── BroadcastConsumer.java          # ③ 广播消费（BROADCASTING）
    │       ├── TagFilterConsumer.java          # ④ Tag 过滤消费
    │       ├── RetryConsumer.java              # ⑤ 失败重试 + 异常处理
    │       ├── DelayConsumer.java              # ⑥ 延迟消息消费
    │       ├── BatchConsumer.java              # ⑦ 批量消息消费
    │       ├── TransactionConsumer.java        # ⑧ 事务消息消费
    │       └── LifecycleManualAckConsumer.java # ⑨ 生命周期定制 / 位点(ack)控制
    └── resources/
        └── application.yml                     # 完整配置
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

## 八、在 RocketMQ Dashboard 观察（http://172.16.75.106:8080）

- **主题（Topic）**：查看自动创建的 `demo-*-topic`；点「状态」看各队列消息总量、最后更新时间。
- **消费者（Consumer）**：查看各消费组的在线状态、**消费进度（Diff/延迟）**、TPS；`Diff=0` 表示无堆积。
- **消息（Message）**：
  - 按 **Topic + 时间范围** 查询；
  - 按 **Message Key** 精确查询（代码中通过 `RocketMQHeaders.KEYS` 设置，如 `TAGKEY-xxx`）；
  - 点「Message Track」查看消息**投递给了哪些消费组、是否消费成功**。
- **事务消息**：提交前半消息存于 `RMQ_SYS_TRANS_HALF_TOPIC`，提交后才对消费者可见。
- **重试/死信**：可在 Topic 列表看到 `%RETRY%demo-retry-consumer-group`、`%DLQ%demo-retry-consumer-group`。

---

## 九、RocketMQ 延迟级别对照表

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

## 十、常见问题（FAQ）

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

---

## 十一、与父工程的关系

本模块是 `springboot-collection-jdk21` 的子模块，继承公共依赖（lombok 等）与插件管理，但：

- 通过覆盖 `boot-version` 属性 + 自身 `dependencyManagement` **独立锁定 Spring Boot 3.3.5**；
- 使用标准 `spring-boot-starter-web`（非内部 `springboot-starter-web`），**便于单独拷贝运行**。
