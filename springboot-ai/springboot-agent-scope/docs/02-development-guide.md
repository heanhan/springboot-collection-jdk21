# 开发与扩展指南

本文面向第一次接触 AgentScope Java 的开发者，说明如何运行、调试和扩展当前旅游多智能体项目。

## 1. 本地开发流程

### 启动模型服务

当前示例依赖本地 OpenAI 兼容服务：

```text
http://127.0.0.1:8000/v1
```

先确认服务已启动，再执行：

```bash
curl http://127.0.0.1:8000/v1/models
```

返回结果中应包含配置的模型名称。

### 编译打包

在仓库根目录执行：

```bash
mvn -pl springboot-ai/springboot-agent-scope/springboot-agent-scope-travel -am clean package
```

### 启动应用

```bash
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar
```

### 调用接口

```bash
curl -X POST http://localhost:8080/api/travel/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "demo-001",
    "message": "请推荐成都三天两夜的行程"
  }'
```

建议第一次验证使用简单问题，并观察响应中的 `routedAgent` 是否符合预期。

## 2. 代码阅读方法

推荐按照“数据模型 → 提示词 → Agent 创建 → 业务编排 → HTTP 接口”的顺序阅读。

### 第一层：数据模型

先阅读：

- `AgentType`
- `TravelChatRequest`
- `TravelChatResponse`
- `RouteDecision`

这些类决定接口和 Agent 之间传递什么数据。当前使用普通 JavaBean 和显式 Getter/Setter，便于熟悉 JDK 8 的开发者阅读。

### 第二层：提示词

阅读 `TravelAgentPrompts`。

提示词中需要重点关注：

- 路由 Agent 只分类，不直接回答。
- 复合问题选择最主要的目标。
- 专家不得编造实时价格、天气、政策和营业时间。
- 预算专家必须保持用户给出的天数和人数。
- 指南 Agent 只能整理专家材料，不新增事实。

### 第三层：Agent 创建

阅读 `TravelAgentConfiguration`。

需要关注：

- 一个共享的 `OpenAIChatModel`。
- 一个共享的 `JsonFileAgentStateStore`。
- 六个 `ReActAgent`。
- 每个 Agent 的名称、提示词、最大迭代次数和空 `Toolkit`。

### 第四层：业务编排

阅读 `TravelGuideService#chat`。

这是项目核心，依次完成：

1. 调用路由 Agent。
2. 解析 `RouteDecision`。
3. 调用对应专家 Agent。
4. 调用指南 Agent。
5. 组装 `TravelChatResponse`。

### 第五层：HTTP 入口

阅读 `TravelChatController`，理解请求路径和参数校验入口。

## 3. 修改提示词

所有提示词集中在：

`springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/agent/TravelAgentPrompts.java`

修改步骤：

1. 找到对应 Agent 的常量。
2. 明确该 Agent 的输入、输出和禁止事项。
3. 保持路由结果字段与 `RouteDecision` 一致。
4. 重新打包并调用接口验证。

### 路由效果不稳定时

如果复合问题总是路由到 `GENERAL`，检查 `ROUTER` 提示词是否明确说明：

- 以制定攻略或安排行程为主时选择 `ITINERARY`。
- 同时提到美食或预算不应直接转 `GENERAL`。
- `targetAgent` 只能使用 `AgentType` 中存在的值。

### 用户条件被改写时

如果模型把三天两夜改成五天，或自行增加人数，需要同时检查：

- 专家提示词是否要求保持用户条件。
- `TravelGuideService` 传入专家的硬性要求是否足够明确。
- 指南 Agent 是否被要求不得复制与用户条件冲突的数字。

## 4. 新增专家 Agent

假设需要新增“酒店专家”，可以按以下步骤扩展。

### 步骤一：扩展专家类型

在 `AgentType` 中增加枚举值，例如：

```java
HOTEL
```

### 步骤二：增加提示词

在 `TravelAgentPrompts` 中增加酒店专家提示词，明确：

- 负责住宿区域和酒店类型建议。
- 不编造实时房价和空房信息。
- 保持用户目的地、天数、人数和预算条件。

### 步骤三：创建 Agent

在 `TravelAgentConfiguration#travelAgentTeam` 中创建酒店 Agent，并放入专家 Map：

```java
specialists.put(AgentType.HOTEL, createAgent(
        "travel-hotel",
        TravelAgentPrompts.HOTEL,
        model,
        stateStore,
        4));
```

### 步骤四：更新路由提示词

在 `TravelAgentPrompts.ROUTER` 的专家列表中增加 `HOTEL` 的说明，让路由模型知道什么时候选择酒店专家。

### 步骤五：补充默认理由

在 `TravelGuideService#defaultReason` 中增加 `HOTEL` 对应的默认理由。

### 步骤六：验证

分别验证：

- 明确的酒店问题是否路由到 `HOTEL`。
- 复合行程问题是否仍然选择最主要的目标。
- 酒店专家没有返回内容时是否按预期处理。

## 5. 调整模型参数

配置文件：

`springboot-agent-scope-travel/src/main/resources/application.yml`

常用调整：

| 参数 | 调整建议 |
| --- | --- |
| `model-name` | 必须与本地模型服务返回的名称一致 |
| `temperature` | 需要稳定输出时调低，需要多样性时适当调高 |
| `max-tokens` | 回答被截断时适当增加 |
| `timeout-seconds` | 本地模型较慢时适当增加 |
| `stream` | 控制模型内部生成方式，不代表 HTTP SSE |
| `state-directory` | 需要隔离不同环境时修改目录 |

`TravelAgentProperties` 中的字段默认值与 YAML 配置共同决定最终参数。

## 6. 会话与状态调试

每次请求都需要传入 `sessionId`：

```json
{
  "sessionId": "debug-001",
  "message": "继续刚才的成都行程"
}
```

排查上下文问题时：

1. 多轮请求必须使用同一个 `sessionId`。
2. 检查 `.agentscope-data/travel` 是否产生状态文件。
3. 区分路由、专家和指南各自的会话上下文。
4. 如果切换了专家类型，专家会话也会变化，这是当前设计。
5. 需要重新开始时使用新的 `sessionId`，或清理对应本地状态。

当前用户 ID 固定为 `local-demo-user`，不要把它当作完整的用户认证方案。

## 7. 接入实时工具

当前所有 Agent 都使用空 `Toolkit`，不会调用外部工具。

未来可以按以下方向扩展：

- 天气工具：查询目的地日期范围内的天气。
- 地图工具：计算景点间距离和交通时间。
- 搜索工具：获取景点、餐厅和营业信息。
- 票价工具：查询交通和门票价格。

接入时应保持职责清晰：

- 路由 Agent 只负责选择专家。
- 专家 Agent 负责调用本领域工具。
- 指南 Agent 只整理专家材料。
- 对实时信息仍然保留来源和核实提醒。

具体工具注册方式以当前 AgentScope 2.0.4 的 Java API 为准，参考 [AgentScope Java 2.0 文档](https://java.agentscope.io/v2)。

## 8. 常见问题

### 应用启动失败

依次检查：

- JDK 是否为 21。
- 本地模型服务是否启动。
- `base-url` 是否可访问。
- `model-name` 是否与模型列表一致。
- `api-key` 是否满足本地服务要求。
- 配置中的温度和 Token 数是否在校验范围内。

### 路由结果总是 GENERAL

检查：

- `ROUTER` 提示词中的专家定义是否完整。
- 模型是否支持结构化输出。
- 应用日志是否出现 `Router did not return a valid structured decision`。
- `RouteDecision` 是否能够被正确反序列化。

### 回答为空或 HTTP 502

专家回答为空时服务会返回 HTTP 502。可以检查：

- 模型服务是否超时。
- `max-tokens` 是否过小。
- 提示词是否要求模型只调用工具但没有工具。
- 应用日志中的模型调用异常。

### 多轮对话丢失上下文

检查后续请求是否仍使用同一个 `sessionId`。不同专家拥有独立会话，因此跨专家切换时不会直接共享专家内部历史，服务会通过当前问题和路由理由补充必要上下文。

### 模型生成内容不准确

当前 Demo 没有实时工具，本地小模型也可能误解约束。可以：

- 降低 `temperature`。
- 增强提示词中的硬性要求。
- 增加确定性业务校验。
- 对价格、天气、政策等信息接入可靠工具。

## 9. 验证清单

修改完成后至少验证：

- [ ] Maven 干净打包成功。
- [ ] 本地模型列表可以访问。
- [ ] 行程问题路由到 `ITINERARY`。
- [ ] 美食问题路由到 `FOOD`。
- [ ] 预算问题路由到 `BUDGET`。
- [ ] 不明确问题路由到 `GENERAL`。
- [ ] 同一 `sessionId` 可以保留上下文。
- [ ] 非法 `sessionId` 返回 HTTP 400。
- [ ] 响应包含 `notice` 实时信息提醒。
- [ ] 明文密钥没有提交到共享仓库。

## 10. 当前代码风格说明

项目运行环境是 JDK 21，但示例代码尽量采用熟悉 JDK 8 的开发者容易理解的写法：

- 公共数据模型使用普通 JavaBean，而不是 `record`。
- 使用显式字段、构造方法、Getter 和 Setter。
- 配置属性使用字段默认值，便于直接看到默认参数。
- 注释重点解释 Agent 职责、结构化输出、会话隔离和兜底逻辑。

目前 `TravelGuideService` 和 `TravelAgentPrompts` 中仍有文本块、`switch` 表达式等 JDK 21 写法，阅读时应结合 JDK 21 语法理解。
