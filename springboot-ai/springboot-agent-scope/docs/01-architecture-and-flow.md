# 架构与调用流程

本文说明当前旅游 Demo 的模块职责、六个 Agent 的协作方式、结构化输出、会话状态、长期记忆和异常兜底。

> RAG 第 2 阶段已经完成：路由后执行 pgvector 检索，MySQL 正文回查后的材料进入专家和指南 Agent，最终响应返回可追溯的知识来源。当前使用 256 维特征哈希演示 Embedding，向量生成、检索算法和学习方法见 [RAG 学习指南](03-rag-learning-guide.md)。

## 1. 模块边界

```text
springboot-agent-scope-api
├── AgentType
├── RouteDecision
├── TravelChatRequest
├── TravelChatResponse
├── KnowledgeSearchRequest
├── KnowledgeSearchResponse
└── KnowledgeSearchResult

springboot-agent-scope-travel
├── AgentScopeTravelApplication
├── controller
│   ├── TravelChatController
│   └── TravelKnowledgeController
├── service
│   └── TravelGuideService
├── agent
│   ├── TravelAgentPrompts
│   └── TravelAgentTeam
├── rag
│   ├── TravelKnowledgeService
│   │   启动时同步 MySQL 正文并同步 pgvector 向量
│   ├── TravelEmbeddingService
│   ├── DemoFeatureHashEmbeddingService
│   ├── TravelVectorRepository
│   ├── TravelKnowledgeChunk
│   └── TravelKnowledgeChunkRepository
└── config
    ├── TravelAgentConfiguration
    ├── TravelAgentProperties
    ├── TravelRagProperties
    ├── TravelVectorConfiguration
    └── TravelVectorProperties
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

所有 Agent 都由 `TravelAgentConfiguration#createAgent` 创建，当前使用 `HarnessAgent`。Toolkit 仅保留 `memory_search`、`memory_get`、`session_search`、`session_list`、`session_history` 五个记忆读取和会话召回工具；`memory_save`、`wait_async_results` 以及文件、Shell、Web、子 Agent 和 Skills 能力均已关闭。长期记忆统一由 Guide 的自动 hooks 写入。

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
  │ 1. router.call(List<Msg>, RouteDecision.class, routerContext)
  ▼
travel-router
  │
  │ 返回 targetAgent + reason
  ▼
TravelGuideService
  │
  │ 2. knowledgeService.search(message)
  ▼
TravelKnowledgeService
  │
  │ 1. 当前问题生成查询向量
  │ 2. pgvector 按余弦距离取候选
  │ 3. 相似度阈值过滤
  │ 4. 按 chunk_id 从 MySQL 批量读取正文
  │ 5. 保持向量顺序，每来源最多 2 个，截取 Top-K
  │
  └── 返回带稳定顺序和相似度百分比的知识片段
  ▼
TravelGuideService
  │
  │ 3. specialists.get(targetAgent).call(specialistInput, specialistContext)
  │   输入包含 [1]、[2] 等编号的检索材料
  ▼
对应专家 Agent
  │
  │ 返回带引用的专业材料
  ▼
TravelGuideService
  │
  │ 4. guide.call(guideInput, guideContext)
  │   输入同时包含专家材料和检索材料
  ▼
travel-guide
  │
  │ 返回保留引用的最终答案
  ▼
TravelGuideService
  │
  │ 5. 统一追加知识来源，组装 knowledgeSources
  ▼
TravelChatResponse
```

### 第一步：参数校验

`TravelChatController` 使用 `@Valid` 触发 `TravelChatRequest` 中的 Jakarta Validation 注解。

`sessionId` 被限制为字母、数字、下划线和短横线，既保证不同会话可以区分，也避免非法字符影响本地状态文件路径。

### 第二步：路由

`TravelGuideService` 调用：

```java
List<Msg> routeMessages = Collections.singletonList(
    Msg.builder()
        .role(MsgRole.USER)
        .textContent(request.getMessage())
        .build());
agents.getRouter()
    .call(routeMessages, RouteDecision.class, routerContext)
    .block(timeout);
```

这里第二个参数 `RouteDecision.class` 表示希望模型返回结构化对象，而不是普通文本。

`RouteDecision` 包含：

- `targetAgent`：专家类型。
- `reason`：路由理由，同时用于保留目的地、天数、人数等已知条件。

如果模型没有返回有效结构化结果，服务会记录警告并转交 `GENERAL` 专家。

### 第三步：知识检索

服务只把用户当前问题作为检索查询，不拼接路由理由。路由理由中的“不属于旅游、美食或预算”等否定描述会污染查询，因此不参与检索；专家仍可通过自己的会话历史理解多轮追问。

`TravelKnowledgeService` 使用当前唯一的 `TravelEmbeddingService` Bean 生成查询向量，由 `TravelVectorRepository` 在 pgvector 中按 `embedding <=> CAST(? AS vector)` 取回余弦距离候选。应用层按 `similarity = 1 - distance` 计算相似度，先用 `travel.rag.vector.min-similarity` 过滤，再从 MySQL `findAllById` 批量取回正文，按向量顺序保留每来源最多 2 个片段并截取 Top-K。服务为返回片段分配 `[1]`、`[2]` 等顺序编号，`score` 先将相似度限制在 0–1，再四舍五入为 0–100 的相似度百分比。没有命中时，会向 Agent 明确说明未检索到项目内置知识。

### 第四步：专家回答

服务根据 `targetAgent` 从 `TravelAgentTeam#getSpecialists` 中取得对应 Agent。

传给专家的信息包括：

- 当前日期。
- 用户当前问题。
- 路由阶段保留的上下文。
- 独立标注的 `MEMORY.md` 长期记忆，明确禁止添加知识引用。
- 带统一编号的检索知识。
- 优先依据知识回答并保留引用编号的要求。
- 保持目的地、天数、人数不变的硬性要求。
- 不得编造实时天气、价格或政策的限制。

如果专家没有返回有效文本，服务抛出 HTTP 502。

### 第五步：指南整理与来源返回

指南 Agent 接收：

- 用户当前问题。
- 路由上下文。
- 本次接洽的专家类型。
- 独立标注的 `MEMORY.md` 长期记忆，当前用户消息优先。
- 专家生成的完整材料。
- 与专家材料编号一致的检索知识。

指南 Agent 只负责统一结构和表达，不负责新增专家材料之外的事实，并被要求保留知识引用编号。描述用户长期记忆的句子不得添加引用编号，混合内容必须拆句。如果指南 Agent 返回空内容，服务会退回到专家原始回答。Guide 返回后，服务还会启动后台补偿整理，避免首次抽取与维护并发时漏生成 `MEMORY.md`。

最后，程序在答案末尾统一追加“知识来源”章节，并把同一组片段按原顺序写入 `TravelChatResponse.knowledgeSources`。这样即使模型漏写正文引用，响应中的来源仍不会丢失。

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

当前 HTTP 请求使用固定用户 ID `local-demo-user`，因此接口暂时没有真实多用户入口；Harness 工作区本身已按 `userId` 隔离。正式接入认证后，应从认证上下文取得用户 ID，不能信任请求体随意传入的 `userId`。

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

`TravelVectorProperties` 绑定 `travel.rag.vector`，通过 `@Validated` 校验 PostgreSQL 地址、用户名和必填密码。`TravelVectorConfiguration` 为该前缀创建独立的 PostgreSQL `JdbcTemplate`，不会替换 Spring Data JPA 的 MySQL 数据源。当前相似度阈值为 `0.15`，候选上限为 `20`。

## 6. MySQL 与 pgvector 的分工

当前两个数据库都参与应用运行链路：

| 数据库 | 保存内容 | 使用方式 |
| --- | --- | --- |
| MySQL 8 | `travel_knowledge_chunk` 中的 19 个知识片段及后续业务数据 | Spring Data JPA；启动同步正文，检索时按 ID 批量回查 |
| PostgreSQL 17.9 + pgvector 0.8.2 | `travel_knowledge_embedding` 中的 19 条向量 | 独立 JDBC；自动建表、模型版本标记、幂等 upsert、HNSW 余弦检索 |

两库通过 `chunk_id` 在应用层关联，不设置跨数据库外键。向量记录缺失或对应 MySQL 正文不存在时，该候选会被跳过。

启动过程：

1. `TravelVectorRepository` 首次启动自动执行 `CREATE EXTENSION IF NOT EXISTS vector`、创建 `travel_knowledge_embedding` 表、`model_name` 普通索引和 HNSW `vector_cosine_ops` 索引。
2. `TravelKnowledgeService` 读取 `knowledge/travel/*.md` 并分块。
3. 根据稳定片段 ID 查询 MySQL；不存在时插入，标题、来源或正文变化时更新，不变时跳过。
4. 从 MySQL 读取全部片段，对“标题 + 换行 + 正文”计算 SHA-256 `content_hash`。
5. 按 `chunk_id + content_hash + model_name` 判断向量是否最新；缺失或变化时调用同一个 Embedding Bean，并以 `ON CONFLICT (chunk_id)` 幂等写入 pgvector。
6. 问答时生成查询向量，只检索当前 `model_name` 的候选，经阈值、正文回查、来源限制后返回 Top-K。

PostgreSQL 账号必须能创建扩展、表和索引，并读写向量表；pgvector 扩展文件必须已安装到数据库服务器。已有向量表不会随 Embedding 维度自动变更，应用也不会自动删表；启动时若检测到维度不一致，会明确失败并要求人工备份、重建向量表。

当前 `DemoFeatureHashEmbeddingService` 先转小写并过滤“推荐、当地、怎么、如何”等泛化短语，再使用 SHA-256 特征哈希、中文二元词和英文词生成 256 维 L2 归一化向量，模型标记为 `demo-feature-hash-v3`。它只验证工程链路，不具备真正的语义理解。真实模型替换时必须保证文档向量与查询向量使用同一实现和模型版本。

## 7. 兜底策略

| 场景 | 当前处理 |
| --- | --- |
| 路由结果为空或缺少 `targetAgent` | 转交 `GENERAL` 专家 |
| 路由理由为空 | 使用专家类型对应的默认理由 |
| 未检索到知识 | 专家收到明确提示，不生成知识引用；响应来源为空数组 |
| 专家回答为空 | 返回 HTTP 502 |
| 指南回答为空 | 使用专家回答作为最终答案，仍保留知识来源 |
| 模型调用超时 | Reactor `block(timeout)` 抛出异常，由 Spring 默认错误处理返回 |

所有成功响应都会附带提醒：本地模型没有实时查询交通、票价、天气或营业信息，出行前需要通过官方渠道核实。

## 8. HarnessAgent 与长期记忆

当前六个 Agent 都使用 AgentScope Java 2.0.4 的 `HarnessAgent`。选择它的主要原因是核心 `ReActAgent` 的旧长期记忆 API 已弃用，而 Harness 提供工作区隔离、两层长期记忆、会话召回和可关闭的中间件能力。

长期记忆分为两层：

| 文件 | 职责 | 写入方 |
| --- | --- | --- |
| `memory/YYYY-MM-DD.md` | 每次 Guide 调用结束后异步追加的每日日志 | `MemoryFlushManager` |
| `MEMORY.md` | LLM 定期合并、去重后的跨天记忆 | `MemoryConsolidator` |
| `memory/.consolidation_state` | 最近一次成功整理的时间水印 | `MemoryConsolidator` |

默认目录为：

```text
.agentscope-data/travel-memory/
└── <userId>/
    ├── MEMORY.md
    └── memory/
        ├── YYYY-MM-DD.md
        └── .consolidation_state
```

`LocalFilesystemSpec.isolationScope(IsolationScope.USER)` 保证同一用户跨会话共享记忆，不同用户使用不同目录。路由和专家通过 `disableMemoryHooks()` 关闭自动写入，只读取记忆；只有 Guide 开启自动抽取和整理。所有 Agent 都移除了 `memory_save`，避免模型主动保存和自动 hooks 重复写入。

记忆抽取和整理是异步的，当前 HTTP 请求不会等待文件写完。首次整理在首次符合条件时执行，`consolidationMinGap` 的 30 分钟是后续两次整理之间的最小间隔，不是首次延迟。由于 Harness 2.0.4 的首次抽取和维护可能并发，`TravelGuideService` 会等待后台任务静默后按水印补偿一次整理，避免维护先读到空日志。验证跨会话记忆时需要等待或轮询文件，不能请求返回后立即断言。

旅游提示词要求只记录用户明确表达且未来可复用的偏好、约束、历史决策和必要事实；当前用户输入优先于历史记忆，密码、证件号、支付信息等敏感内容不进入长期记忆。长期记忆属于用户个人上下文，不是 RAG 公共知识，不能添加 `[1]`、`[2]` 等检索引用。

详细配置、验证步骤和排障方法见 [长期记忆学习指南](04-long-term-memory-guide.md)。

## 9. 关键源码入口

- [TravelAgentConfiguration.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelAgentConfiguration.java)
- [TravelAgentPrompts.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/agent/TravelAgentPrompts.java)
- [TravelGuideService.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/service/TravelGuideService.java)
- [TravelKnowledgeService.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/rag/TravelKnowledgeService.java)
- [TravelEmbeddingService.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/rag/TravelEmbeddingService.java)
- [DemoFeatureHashEmbeddingService.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/rag/DemoFeatureHashEmbeddingService.java)
- [TravelVectorRepository.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/rag/TravelVectorRepository.java)
- [TravelChatController.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/controller/TravelChatController.java)
- [TravelKnowledgeController.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/controller/TravelKnowledgeController.java)
- [TravelAgentProperties.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelAgentProperties.java)
- [TravelRagProperties.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelRagProperties.java)
- [TravelVectorConfiguration.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelVectorConfiguration.java)
- [TravelVectorProperties.java](../springboot-agent-scope-travel/src/main/java/com/example/agentscope/travel/config/TravelVectorProperties.java)
