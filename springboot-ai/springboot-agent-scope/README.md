# Spring Boot + AgentScope 旅游多智能体示例

本项目使用 **AgentScope Java 2.0.4** 和 **Spring Boot 3.5.16**，通过本地 OpenAI 兼容模型实现一个旅游问答 Demo：

1. 客服路由 Agent 判断用户意图。
2. 行程、美食、预算或通用专家 Agent 生成专业材料。
3. 旅游指南 Agent 把专家材料整理为统一回复。

项目基于 JDK 21 构建，代码风格面向熟悉 JDK 8 的开发者，但当前依赖要求 JDK 21，不能直接使用 JDK 8 运行。

## 技术栈

| 技术 | 版本或用途 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| AgentScope Java | 2.0.4 |
| 模型接入 | OpenAI 兼容 Chat Completions API |
| 本地模型示例 | `Qwen3.5-4B-MLX-4bit` |
| 状态存储 | `JsonFileAgentStateStore` |
| 接口风格 | HTTP JSON，非 SSE 流式接口 |

> 本项目是 AgentScope Java 示例。`docs.agentscope.io/zh/versions/2.0.12` 对应 Python 版文档，Java API 应以 [AgentScope Java 2.0 文档](https://java.agentscope.io/v2) 为准。

## 模块结构

```text
springboot-agent-scope
├── pom.xml
├── springboot-agent-scope-api
│   └── 请求、响应、路由结果和专家类型
├── springboot-agent-scope-travel
│   ├── controller
│   ├── service
│   ├── agent
│   └── config
└── docs
    ├── 01-architecture-and-flow.md
    └── 02-development-guide.md
```

### API 模块

`springboot-agent-scope-api` 只保存旅游场景的公共数据模型：

- `AgentType`：专家类型枚举。
- `TravelChatRequest`：HTTP 请求参数。
- `TravelChatResponse`：HTTP 响应结果。
- `RouteDecision`：路由 Agent 的结构化输出。

### Travel 模块

`springboot-agent-scope-travel` 负责实际运行：

- `TravelChatController`：提供 `/api/travel/chat`。
- `TravelGuideService`：编排路由、专家和指南 Agent。
- `TravelAgentConfiguration`：创建模型、状态存储和六个 Agent。
- `TravelAgentPrompts`：集中管理六个 Agent 的提示词。
- `TravelAgentProperties`：绑定模型、超时和状态目录配置。

## 整体调用链

```text
POST /api/travel/chat
        │
        ▼
TravelChatController
        │  参数校验
        ▼
TravelGuideService
        │
        ├── 1. travel-router
        │      输出 RouteDecision
        │
        ├── 2. specialists[targetAgent]
        │      ITINERARY / FOOD / BUDGET / GENERAL
        │
        └── 3. travel-guide
               整理专家材料
        │
        ▼
TravelChatResponse
```

六个 Agent 共享同一个模型实例和状态存储，但使用不同的 Agent 名称、提示词和会话命名空间。

## 快速启动

### 1. 准备本地模型服务

启动 OpenAI 兼容服务，并确认以下地址可访问：

```text
Base URL: http://127.0.0.1:8000/v1
Model:    Qwen3.5-4B-MLX-4bit
```

可以检查模型列表：

```bash
curl http://127.0.0.1:8000/v1/models
```

### 2. 检查配置

配置文件：

`springboot-agent-scope-travel/src/main/resources/application.yml`

主要配置项：

| 配置项 | 说明 |
| --- | --- |
| `travel.agent.api-key` | 本地服务密钥 |
| `travel.agent.base-url` | OpenAI 兼容接口地址 |
| `travel.agent.model-name` | 模型名称 |
| `travel.agent.stream` | 模型内部是否流式生成 |
| `travel.agent.temperature` | 生成随机程度 |
| `travel.agent.max-tokens` | 单次回答最大 Token 数 |
| `travel.agent.timeout-seconds` | 单次 Agent 调用超时时间 |
| `travel.agent.state-directory` | 会话状态保存目录 |

当前示例配置用于本地学习。共享代码前应将明文密钥移除，并改为环境变量或外部配置中心管理。

### 3. 构建

在仓库根目录执行，使用 JDK 21：

```bash
mvn -pl springboot-ai/springboot-agent-scope/springboot-agent-scope-travel -am clean package
```

### 4. 启动

```bash
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar
```

默认端口为 `8080`。需要切换端口时：

```bash
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar --server.port=18080
```

## 接口示例

### 请求

```http
POST /api/travel/chat
Content-Type: application/json
```

```json
{
  "sessionId": "chengdu-001",
  "message": "我和朋友两个人去成都玩三天两夜，请安排行程，并介绍当地美食"
}
```

### 响应结构

```json
{
  "sessionId": "chengdu-001",
  "routedAgent": "ITINERARY",
  "routeReason": "用户主要目标是制定成都三天两夜行程。",
  "answer": "这里返回整理后的 Markdown 旅游指南。",
  "notice": "本指南由本地模型生成，未实时查询交通、票价、天气或营业信息，出行前请通过官方渠道核实。"
}
```

`routedAgent` 可能为：

- `ITINERARY`：行程规划专家。
- `FOOD`：美食顾问。
- `BUDGET`：预算顾问。
- `GENERAL`：通用旅游顾问。

### curl 调用

```bash
curl -X POST http://localhost:8080/api/travel/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "chengdu-001",
    "message": "我和朋友两个人去成都玩三天两夜，请安排行程，并介绍当地美食"
  }'
```

### 多轮对话

后续问题继续使用相同 `sessionId`：

```json
{
  "sessionId": "chengdu-001",
  "message": "这次旅行大概需要多少预算？"
}
```

路由 Agent、专家 Agent 和指南 Agent 会分别从自己的会话上下文中读取历史信息，因此可以保留成都、三天两夜、两个人等条件。

## 请求校验

- `sessionId` 不能为空，最长 100 个字符。
- `sessionId` 只能包含字母、数字、下划线和短横线。
- `message` 不能为空，最长 2000 个字符。
- 不符合规则时返回 HTTP 400。

## 推荐代码阅读顺序

1. `AgentType`：先理解四种专家类型。
2. `TravelChatRequest`、`TravelChatResponse`：理解接口契约。
3. `TravelAgentPrompts`：理解每个 Agent 的职责边界。
4. `TravelAgentConfiguration`：理解 Agent 是如何创建的。
5. `TravelGuideService`：理解多 Agent 编排和兜底逻辑。
6. `TravelChatController`：理解 HTTP 入口。
7. `TravelAgentProperties`：理解可调整的运行参数。

## 当前边界

- 没有接入天气、地图、票价、搜索等实时工具。
- 固定使用 `local-demo-user`，没有真实登录和用户隔离。
- 模型配置支持流式生成，但 HTTP 接口仍返回聚合后的 JSON。
- 当前没有自动化测试源码，主要通过编译、打包和接口联调验证。
- 本地小模型可能改变用户给出的天数、人数或生成不准确信息，回复中会附带核实提醒。

## 延伸阅读

- [架构与调用流程](docs/01-architecture-and-flow.md)
- [开发与扩展指南](docs/02-development-guide.md)
- [AgentScope Java 2.0 官方文档](https://java.agentscope.io/v2)
