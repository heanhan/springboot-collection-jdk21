# 架构与调用流程

本文说明当前旅游 Demo 的模块职责、六个 Agent 的协作方式、结构化输出、会话状态和异常兜底。

## 1. 模块边界

```text
springboot-agent-scope-api
├── AgentType
├── RouteDecision
├── TravelChatRequest
└── TravelChatResponse

springboot-agent-scope-travel
├── AgentScopeTravelApplication
├── controller
│   └── TravelChatController
├── service
│   └── TravelGuideService
├── agent
│   ├── TravelAgentPrompts
│   └── TravelAgentTeam
└── config
    ├── TravelAgentConfiguration
    └── TravelAgentProperties
```

API 模块不依赖 AgentScope，只负责公共数据结构。Travel 模块负责模型接入、Agent 创建、状态存储和 HTTP 业务编排。

## 2. 六个 Agent

| Agent 名称 | 职责 | 最大迭代次数 | 输出方式 |
| --- | --- | --- | --- |
| `travel-router` | 判断用户当前问题应交给哪位专家 | 3 | `RouteDecision` 结构化对象 |
| `travel-itinerary` | 目的地、路线、景点、交通和住宿建议 | 4 | Markdown 文本 |
| `travel-food` | 当地美食、用餐区域和饮食注意事项 | 4 | Markdown 文本 |
| `travel-budget` | 交通、住宿、餐饮、门票和弹性费用估算 | 4 | Markdown 文本 |
| `travel-general` | 处理不明确或无法归类的旅游问题 | 4 | Markdown 文本 |
| `travel-guide` | 将专家材料整理为统一旅游指南 | 4 | Markdown 文本 |

所有 Agent 都由 `TravelAgentConfiguration#createAgent` 创建，当前使用空的 `Toolkit`，因此不具备外部工具调用能力。

## 3. 请求处理时序

```text
游客
  │
  │ POST /api/travel/chat
  ▼
TravelChatController
  │
  │ @Valid 校验 sessionId 和 message
  ▼
TravelGuideService#chat
  │
  │ 1. router.call(message, RouteDecision.class, routerContext)
  ▼
travel-router
  │
  │ 返回 targetAgent + reason
  ▼
TravelGuideService
  │
  │ 2. specialists.get(targetAgent).call(specialistInput, specialistContext)
  ▼
对应专家 Agent
  │
  │ 返回专业材料
  ▼
TravelGuideService
  │
  │ 3. guide.call(guideInput, guideContext)
  ▼
travel-guide
  │
  │ 返回最终答案
  ▼
TravelChatResponse
```

### 第一步：参数校验

`TravelChatController` 使用 `@Valid` 触发 `TravelChatRequest` 中的 Jakarta Validation 注解。

`sessionId` 被限制为字母、数字、下划线和短横线，既保证不同会话可以区分，也避免非法字符影响本地状态文件路径。

### 第二步：路由

`TravelGuideService` 调用：

```java
agents.getRouter()
    .call(request.getMessage(), RouteDecision.class, routerContext)
    .block(timeout);
```

这里第二个参数 `RouteDecision.class` 表示希望模型返回结构化对象，而不是普通文本。

`RouteDecision` 包含：

- `targetAgent`：专家类型。
- `reason`：路由理由，同时用于保留目的地、天数、人数等已知条件。

如果模型没有返回有效结构化结果，服务会记录警告并转交 `GENERAL` 专家。

### 第三步：专家回答

服务根据 `targetAgent` 从 `TravelAgentTeam#getSpecialists` 中取得对应 Agent。

传给专家的信息包括：

- 当前日期。
- 用户当前问题。
- 路由阶段保留的上下文。
- 保持目的地、天数、人数不变的硬性要求。
- 不得编造实时天气、价格或政策的限制。

如果专家没有返回有效文本，服务抛出 HTTP 502。

### 第四步：指南整理

指南 Agent 接收：

- 用户当前问题。
- 路由上下文。
- 本次接洽的专家类型。
- 专家生成的完整材料。

指南 Agent 只负责统一结构和表达，不负责新增专家材料之外的事实。如果指南 Agent 返回空内容，服务会退回到专家原始回答。

## 4. 会话状态

状态存储使用：

```java
new JsonFileAgentStateStore(properties.getStateDirectory())
```

默认目录：

```text
.agentscope-data/travel
```

每个 Agent 使用独立的会话命名空间：

| Agent | 会话 ID 规则 |
| --- | --- |
| 路由 Agent | `travel-router-${sessionId}` |
| 专家 Agent | `travel-${agentType}-${sessionId}` |
| 指南 Agent | `travel-guide-${sessionId}` |

例如游客会话为 `chengdu-001` 时：

```text
travel-router-chengdu-001
travel-itinerary-chengdu-001
travel-budget-chengdu-001
travel-guide-chengdu-001
```

这样可以避免路由 Agent 和专家 Agent 的上下文互相污染。`.agentscope-data/` 已加入 `.gitignore`，运行产生的状态文件不会提交到 Git。

当前所有请求使用固定用户 ID `local-demo-user`，因此该 Demo 只适合本地学习，不具备真实多用户隔离能力。

## 5. 模型与配置

`TravelAgentConfiguration#travelModel` 使用 `OpenAIChatModel`，把本地 OpenAI 兼容服务封装为 AgentScope 的 `Model`。

主要参数：

- `apiKey`
- `baseUrl`
- `modelName`
- `stream`
- `temperature`
- `maxTokens`
- `parallelToolCalls(false)`

六个 Agent 共享同一个 `Model` Bean，避免重复创建模型客户端。

`TravelAgentProperties` 使用 `@ConfigurationProperties` 绑定 `travel.agent` 前缀，并通过 `@Validated` 校验密钥、温度、Token 数和超时时间。

## 6. 兜底策略

| 场景 | 当前处理 |
| --- | --- |
| 路由结果为空或缺少 `targetAgent` | 转交 `GENERAL` 专家 |
| 路由理由为空 | 使用专家类型对应的默认理由 |
| 专家回答为空 | 返回 HTTP 502 |
| 指南回答为空 | 使用专家回答作为最终答案 |
| 模型调用超时 | Reactor `block(timeout)` 抛出异常，由 Spring 默认错误处理返回 |

所有成功响应都会附带提醒：本地模型没有实时查询交通、票价、天气或营业信息，出行前需要通过官方渠道核实。

## 7. 为什么使用 ReActAgent

当前示例使用 `ReActAgent`，即使没有注册工具，也可以复用 AgentScope 的标准消息、模型调用、状态和结构化输出机制。

未来接入工具时，可以在现有 Agent 上注册 `Toolkit` 工具，让 Agent 根据模型判断是否调用天气、地图或搜索能力。当前版本的空 `Toolkit` 表示不会执行任何外部工具。

## 8. 关键源码入口

- [TravelAgentConfiguration.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelAgentConfiguration.java)
- [TravelAgentPrompts.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/agent/TravelAgentPrompts.java)
- [TravelGuideService.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/service/TravelGuideService.java)
- [TravelChatController.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/controller/TravelChatController.java)
- [TravelAgentProperties.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelAgentProperties.java)
