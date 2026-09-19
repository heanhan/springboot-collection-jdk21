# 运行、接口与验收手册

## 本地运行

构建和启动命令见 [README](../README.md)。八个服务各用独立数据库，默认 MySQL 用户、密码为本地示例值；Redis 使用不同逻辑库，RocketMQ 按 Topic 和消费组隔离。

配置可通过 Spring Boot 环境变量覆盖，无需修改源码：

| 环境变量 | 作用 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | 设为 `dev` 加载本地开发覆盖，默认不激活额外 profile |
| `SPRING_DATASOURCE_URL` | 当前服务 JDBC URL，必须指向该服务自己的 schema |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 当前服务数据库凭据 |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_PASSWORD` | Redis 连接 |
| `ROCKETMQ_NAME_SERVER` | NameServer 地址，默认 localhost:9876 |
| `DDD_SERVICES_ORDER_URL` 等 | Feign 目标地址，例如 http://localhost:8085 |
| `DDD_JWT_SECRET` | auth 的 JWT 签名密钥；非本地环境必须替换示例值 |
| `PAYMENT_MOCK_ENABLED` | 开启认证后的模拟回调，默认 false |
| `LOGISTICS_MOCK_ENABLED` | 开启定时轨迹推进，默认 false |
| `DDD_LOGISTICS_MOCK_TRACK_INTERVAL_SECONDS` | 自动轨迹间隔，默认 30 秒 |
| `DDD_ORDER_PAYMENT_TIMEOUT_MINUTES` | 新订单付款期限，默认 30 分钟 |
| `DDD_ORDER_TIMEOUT_SCAN_INTERVAL_MS` | 超时订单扫描周期，默认 60000 毫秒 |
| `DDD_MESSAGING_POLL_MS` | 订单/支付/物流 Outbox 发送扫描周期，默认 1000 毫秒 |

默认配置就在 application.yml，可直接启动。八个服务都提供 application-dev.yml；启动时设置 `SPRING_PROFILES_ACTIVE=dev` 或追加 `--spring.profiles.active=dev` 即可加载。
开发覆盖继承主配置的独立 schema、Redis 库和本地连接地址；统一 Feign 连接/读取超时为 3/5 秒，关闭 Feign 请求日志和 SQL 绑定值输出，健康检查不暴露组件详情，不自动启用支付回调或物流模拟。
各服务 logback-spring.xml 输出带时间、级别、服务名和线程的控制台日志，等级仍由 `logging.level.*` 配置；不自动创建日志文件。
`/actuator/health` 用于存活和数据库/Redis 健康检查，不保证 MQ 业务消费链路已正常。
NameServer 可连接不代表 Broker 地址可达：本地 Docker Broker 广播地址应为宿主机可访问的地址，见 docker/rocketmq/broker.conf。

## 统一响应与认证

响应为 `{ "code": "0000", "message": "...", "data": ..., "traceId": ... }`，空字段可能省略。
成功码是字符串 `0000`。失败必须同时检查 HTTP 状态和业务 code；不能仅根据 HTTP 200 判断成功。
订单、支付、物流、购物车的用户接口要求 `Authorization: Bearer <accessToken>`，用户 ID 不接受请求体覆盖。

`/internal/**` 用于 Feign，当前假定可信内网，未实现服务间认证；购物车内部查询使用 `X-User-Id`。
早期商品/库存/用户运营接口也不应暴露公网。自定义权限 AOP 目前位于 auth 上下文，不是覆盖所有服务的网关鉴权。

## 阶段4～6 用户接口

以下路径以相应服务端口为基址，示例 ID 请使用真实返回值。

| 服务 | 方法 / 路径 | 请求体或说明 |
|---|---|---|
| order | POST `/orders` | 见下单请求示例，返回订单 ID |
| order | GET `/orders/{id}` | 本人订单，返回订单快照 |
| order | GET `/orders?status=CREATED&page=1&size=20` | 本人订单列表，size 1～100 |
| order | POST `/orders/{id}/cancel` | `{"reason":"不需要了"}`，仅未支付订单可取消 |
| order | POST `/orders/{id}/complete` | 本人已发货订单确认收货 |
| payment | GET `/payment/by-order/{orderId}` | 支付单异步创建，需有限轮询 |
| payment | POST `/payment/{id}/pay` | `{"channel":"MOCK"}`，还支持 ALIPAY/WECHAT（均为模拟） |
| payment | POST `/payment/mock/callback` | `{"paymentId":"...","amount":8999.00,"tradeNo":"唯一模拟流水"}` |
| payment | POST `/payment/{id}/refund` | `{"reason":"全额退款"}`，返回退款快照；重复请求返回同一退款单 |
| logistics | GET `/logistics/by-order/{orderId}` | 发货单列表，当前最多一单 |
| logistics | GET `/logistics/{id}` | 含轨迹与商品快照 |
| logistics | POST `/logistics/{id}/deliver` | 本人签收；可重复调用 |
| cart | POST `/cart/items` | `{"skuId":"2000","quantity":1}`，相同 SKU 合并数量 |
| cart | PUT `/cart/items/{skuId}` | `{"quantity":2,"checked":true}` |
| cart | DELETE `/cart/items/{skuId}` | 移除条目 |
| cart | DELETE `/cart/items` | 清空当前用户购物车 |
| cart | GET `/cart/items` | 刷新商品信息和价格 |
| cart | GET `/cart/preview` | 仅计算选中且在售商品；满 99 包邮，否则运费 10 |

下单示例（金额由订单服务重新获取商品价格并计算，不接收客户端成交价）：

```json
{
  "items": [{"skuId": "2000", "quantity": 1}],
  "shippingAddress": {
    "province": "北京市", "city": "北京市", "district": "朝阳区",
    "detail": "示例路1号", "zipCode": "100000",
    "receiver": "测试用户", "mobile": "13800000000"
  },
  "remark": "联调测试"
}
```

一次请求中重复 SKU 会被拒绝，请合并数量。下单接口没有客户端幂等键，网络结果不明时不可盲目重试。
购物车预览不是成交承诺；客户端下单成功后显式删除相应条目，服务不会自动清空。
物流签收接口驱动 `ShipmentDelivered` 事件，适合验证完整链路；订单直接确认收货不会反向推进物流状态。

## 一致性与业务边界

- order/payment/logistics 在业务事务内写 Outbox。Broker 返回 SEND_OK 后才标记已发送，异常和非成功状态保留重试。
- 消费完成标记在业务回调正常返回后写入；内部吞掉异常时，正常返回不代表业务成功。Redis 租约超时或去重标记过期后仍需数据库唯一键、行锁和状态幂等兜底。
- 消息可能乱序；前置状态尚未到达等异常需要传播给 MQ 才能进入失败重试路径，不能提前记录成功。
- 库存以业务号持有取消屏障，先释放后预占被拒绝；同一业务号已实扣后 release 不增加可用库存。
- 正常取消提交后通过 MQ 触发预占释放。当前 `InventoryGatewayImpl.release` 对远程失败只记日志，可能导致取消/退款消费被标为完成而库存未释放；不能保证这类失败由 MQ 自动重试，详见 [学习指南第 7.7 节](ddd-beginner-guide.md#77-邮局比喻的边界当前实现仍有缺口)。
- 订单保存失败会尝试即时 RPC 补偿，但进程崩溃、RPC 失败仍需持久化补偿或对账能力；当前未引入持久化 Saga，不应仅凭代码注释认定完整对账已实现。
- 支付只支持全额退款。支付成功后即允许退款，包括订单已经完成；重复退款不调用第二次渠道。
- 已实扣后的退款不自动补库存，应在实际退货验收后人工入库。当前不包含退货物流完整流程。
- 物流自动任务会检查退款状态并拦截活动发货单；默认关闭时不会后台推进，已完成发货单不自动改写。
- auth/user/product/inventory 的早期消息机制尚未统一为 Outbox；不要将新链路的可靠性保证泛化至整个项目。

## 测试与验收

```bash
# 仓库根目录：所有 DDD 子模块，包含测试与可执行包
mvn -f springboot-ddd-project/pom.xml verify

# 同时验证本地开发配置（H2 测试仍覆盖数据源，不连接真实 MySQL）
mvn -f springboot-ddd-project/pom.xml verify -Dspring.profiles.active=dev

# 单个服务及其共享依赖
mvn -f springboot-ddd-project/pom.xml -pl ddd-payment-service -am test

# 只检查依赖服务，无业务写入
bash springboot-ddd-project/scripts/e2e.sh --check

# 完整真实 HTTP/MQ 联调，支付服务需先启用 Mock
bash springboot-ddd-project/scripts/e2e.sh
```

本地自动回归覆盖：Money、事件序列化、订单状态与超时查询、库存守恒/取消屏障、支付并发回调/重复退款、物流状态/JSON 重建、购物车边界与实时预览、JPA/Flyway 映射、业务与 Outbox 回滚、Broker 失败重试、消费成功标记时机。
H2 测试使用原始 Flyway 脚本和独立内存库，不修改本地 MySQL。它不替代 MySQL 隔离级别、Redis 租约 Lua、真实 MQ 重投和服务启动的集成验收。

脚本阶段与环境要求：
- `e2e-2.sh`：auth + user，异步建档、登录、刷新、旧刷新令牌拒绝、注销失效。
- `e2e-3.sh`：product + inventory，验证示例商品和重复预占/释放、取消屏障。
- `e2e-4.sh`：auth + user + product + inventory + order，下单后取消并等待库存恢复。
- `e2e-5.sh`：再加 payment + logistics，验证关单、重复回调、发货实扣、签收、全额退款。
- `e2e-6.sh` / `e2e.sh`：八服务齐备，追加购物车操作和价格预览校验。

异步轮询默认 90 秒，可设置 `E2E_TIMEOUT=180`；脚本用 `SKU_ID`（默认 2000）、`WAREHOUSE_ID`（阶段3默认 1）指定测试数据。
URL 环境变量如 `ORDER_URL` 只改变脚本目标；还必须在服务环境配置对应的 `DDD_SERVICES_*_URL`。
请串行运行，避免其他请求改动同一 SKU 导致库存基线断言失败。脚本失败会留下可追踪记录，不自动清库。

## 排障

1. `--check` 连接失败：先启动对应服务，检查 MySQL/Redis；服务启动失败检查 Flyway 与 Hibernate validate 日志。
2. 支付单迟迟不创建：检查 order 库 `t_event_outbox.sent=0`、Broker 路由、`ddd-payment-orders` 消费组和死信。
3. 已付款但未发货：检查 payment 成功事件、order 的 PAID 状态与 OrderPaid Outbox、`ddd-logistics-paid` 消费组，再检查库存业务状态。
4. 取消后未释放：检查 `ddd-order-stock-release` 消费组、库存 `t_stock_operation` 与 `t_stock_transaction`、订单端库存适配器的失败日志及 Inbox 完成标记；适配器吞错可能使 MQ 不再进入失败重试路径，不要直接篡改库存流水。
5. Outbox 持续堆积：检查 Broker 地址、Topic、发送状态和失败日志；修复原因后自动重试。已发送行保留，生产应制定归档清理策略。
6. Flyway 已应用脚本不能修改校验和绕过验证；新增迁移升级，或只在明确可丢弃的测试库重建环境。

## DDD 文件索引

路径均相对 springboot-ddd-project：

| 概念 | 示例路径 |
|---|---|
| 值对象 | `ddd-common/src/main/java/com/example/ddd/common/domain/valueobject/Money.java` |
| 聚合/实体 | `ddd-order-service/src/main/java/com/example/ddd/order/domain/model/aggregate/Order.java`、`domain/model/entity/OrderItem.java` |
| 状态机 | `ddd-order-service/src/main/java/com/example/ddd/order/domain/statemachine/OrderStateMachine.java` |
| 领域服务 | `ddd-inventory-service/src/main/java/com/example/ddd/inventory/domain/service/StockReservationService.java` |
| 应用服务 | `ddd-payment-service/src/main/java/com/example/ddd/payment/application/service/PayApplicationService.java` |
| 仓储接口/实现 | `ddd-cart-service/src/main/java/com/example/ddd/cart/domain/repository/CartRepository.java`、`infrastructure/persistence/repository/CartRepositoryImpl.java` |
| 防腐层 | `ddd-order-service/src/main/java/com/example/ddd/order/application/port/ProductGateway.java` |
| 发布语言/领域事件 | `ddd-api-contract/src/main/java/com/example/ddd/contract/order/event/OrderPaidEvent.java` |
| PO 转换 | `ddd-logistics-service/src/main/java/com/example/ddd/logistics/infrastructure/persistence/converter/ShipmentConverter.java` |
