# 开发与扩展指南

本文面向第一次接触 AgentScope Java 的开发者，说明如何运行、调试和扩展当前旅游多智能体项目。

## 1. 本地开发流程

### 启动模型服务

当前示例依赖本地 OpenAI 兼容服务同时提供 Chat 和 Embedding：

```text
http://127.0.0.1:8000/v1
Chat:      Qwen3.6-35B-A3B-5bit
Embedding: Qwen3-Embedding-0.6B
```

先确认服务已启动，再执行：

```bash
export OPENAI_API_KEY='<model-api-key>'
curl http://127.0.0.1:8000/v1/models \
  -H "Authorization: Bearer ${OPENAI_API_KEY}"
```

本机 omlx 服务要求认证；返回结果中应包含上述两个模型名称。密钥只在当前 Shell 中使用，不写入配置文件。

### 编译打包

在仓库根目录执行：

```bash
mvn -pl springboot-ai/springboot-agent-scope/springboot-agent-scope-travel -am clean package
```

### 启动应用

先准备 MySQL 8 业务库、PostgreSQL 17.9 和目标数据库，并确认 PostgreSQL 服务器已安装 pgvector 0.8.2。以下环境变量均为占位符：

```bash
export MYSQL_DATABASE='<mysql-database>'
export MYSQL_USER='<mysql-user>'
export MYSQL_PASSWORD='<mysql-password>'
export PG_URL='jdbc:postgresql://<pg-host>:5432/<pg-database>'
export PG_USER='<pg-user>'
export PG_PASSWORD='<pg-password>'
export OPENAI_API_KEY='<chat-model-api-key>'
export EMBEDDING_API_KEY='<embedding-model-api-key>'
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar
```

MySQL 主机地址通过 `spring.datasource.url` 配置或启动参数覆盖；数据库名和用户名分别由 `MYSQL_DATABASE`、`MYSQL_USER` 覆盖。`MYSQL_PASSWORD` 和 `PG_PASSWORD` 都是启动必填项，不要把真实密码写入配置文件或提交到仓库。`OPENAI_API_KEY` 用于 Chat，`EMBEDDING_API_KEY` 用于 Embedding；本机 omlx 两个接口都需要认证，其他不要求认证的 Embedding 服务可以不设置后者。Embedding 地址、模型和维度分别可通过 `EMBEDDING_BASE_URL`、`EMBEDDING_MODEL_NAME`、`EMBEDDING_DIMENSION` 覆盖。

应用首次启动时，`TravelVectorRepository` 会自动执行 `CREATE EXTENSION IF NOT EXISTS vector`、创建 `travel_knowledge_embedding` 表、`model_name` 普通索引和 HNSW `vector_cosine_ops` 索引。PostgreSQL 账号需要能连接目标数据库、创建扩展、建表、建索引并读写向量表；扩展文件需要预先安装在服务器上，具体权限取决于安装方式。

随后应用把 Markdown 片段同步到 MySQL，按照 `chunk_id + content_hash + model_name` 判断向量是否最新，并将缺失或变化的向量幂等写入 pgvector。当前种子数据为 19 个片段、19 条 1024 维 `Qwen3-Embedding-0.6B@1024` 向量。重复启动不会重复插入相同片段 ID，也不会重复生成未变化向量，当前实测第二次启动同步数量为 0。已有向量表不会自动改变维度；启动时若发现现有向量维度与当前 Embedding 不一致，会明确终止启动，避免误写，应用不会自动删表。

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

独立检索不调用 Chat 模型，可单独验证 pgvector 与 MySQL 链路：

```bash
curl -X POST http://localhost:8080/api/travel/knowledge/search \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "成都三天两夜有什么美食推荐",
    "topK": 3
  }'
```

返回的 `score` 是 0–100 的相似度百分比，不是概率。未知问题应返回空 `results` 和 `totalMatches: 0`。数据库核对 SQL 见 [RAG 学习指南](03-rag-learning-guide.md#9-核对-mysql-与-pgvector-数据)。

## 2. 代码阅读方法

推荐按照“数据模型 → 提示词 → Agent 创建 → 业务编排 → HTTP 接口”的顺序阅读。

### 第一层：数据模型

先阅读：

- `AgentType`
- `TravelChatRequest`
- `TravelChatResponse`
- `RouteDecision`
- `KnowledgeSearchRequest`
- `KnowledgeSearchResponse`
- `KnowledgeSearchResult`

这些类决定接口和 Agent 之间传递什么数据。当前使用普通 JavaBean 和显式 Getter/Setter，便于熟悉 JDK 8 的开发者阅读。

### 第二层：提示词

阅读 `TravelAgentPrompts`。

提示词中需要重点关注：

- 路由 Agent 只分类，不直接回答。
- 复合问题选择最主要的目标。
- 专家不得编造实时价格、天气、政策和营业时间。
- 预算专家必须保持用户给出的天数和人数。
- 指南 Agent 只能整理专家材料和检索知识，不新增事实，并保留知识引用编号。

### 第三层：Agent 创建

阅读 `TravelAgentConfiguration`。

需要关注：

- 一个共享的 `OpenAIChatModel`。
- 一个共享的 `JsonFileAgentStateStore`。
- 六个 `HarnessAgent`。
- 每个 Agent 的名称、提示词、最大迭代次数和用户隔离的记忆工作区。
- 六个 Agent 共享同一个 `TravelRuntimeContextMiddleware`，在每次推理前临时注入运行时信息。
- 仅保留记忆读取与会话召回工具，不暴露主动保存、文件、Shell、Web、子 Agent 和 Skills 能力。

### 第四层：业务编排

阅读 `TravelGuideService#chat`。

这是项目核心，依次完成：

1. 创建本次请求的 `TravelRequestContext`，再为路由、专家和指南分别创建 `RuntimeContext`。
2. 调用路由 Agent。
3. 解析 `RouteDecision`。
4. 只使用用户当前问题检索知识，避免路由理由污染查询。
5. `TravelKnowledgeService` 生成查询向量，从 pgvector 取候选，经阈值过滤后从 MySQL 批量取回正文。
6. 按向量顺序、每来源最多 2 个和 Top-K 规则组装带编号的知识材料。
7. 把知识材料交给对应专家 Agent，再把专家材料和检索知识交给指南 Agent。
8. 统一追加知识来源并组装 `TravelChatResponse`。

### 第五层：RAG 数据链路

阅读 `TravelEmbeddingService`、`OpenAiCompatibleEmbeddingService`、`TravelEmbeddingConfiguration`、`TravelVectorRepository` 和 `TravelVectorConfiguration`。

需要关注：

- MySQL 保存正文，PostgreSQL 保存向量，两库通过 `chunk_id` 关联。
- 默认真实 Bean 的模型标记为 `Qwen3-Embedding-0.6B@1024`，维度为 1024；演示 Bean 仅在 `demo-embedding` Profile 下启用。
- `embedDocument` 直接处理知识正文，`embedQuery` 添加 Qwen3 查询指令，两个入口仍使用同一模型和维度。
- 启动时按 `chunk_id + content_hash + model_name` 判断是否最新，通过 `ON CONFLICT (chunk_id)` 幂等写入。
- 查询 SQL 使用 `embedding <=> CAST(? AS vector)`，应用层将余弦距离转换为 `1 - distance`。
- `score` 先将相似度限制在 0–1，再换算为 0–100 的相似度百分比；候选阈值和候选数量由 `travel.rag.vector` 配置。

### 第六层：HTTP 入口

阅读 `TravelChatController` 和 `TravelKnowledgeController`，理解旅游问答、独立检索两条入口及参数校验。

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
        properties.getMemoryWorkspaceDirectory(),
        memoryConfig,
        contextMiddleware,
        4,
        false));
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
| `travel.agent.api-key` | 通过 `OPENAI_API_KEY` 环境变量注入，不写入配置文件 |
| `travel.agent.base-url` | 本地 OpenAI 兼容服务地址 |
| `travel.agent.model-name` | 必须与本地 Chat 模型服务返回的名称一致 |
| `travel.agent.temperature` | 需要稳定输出时调低，需要多样性时适当调高 |
| `travel.agent.max-tokens` | 回答被截断时适当增加 |
| `travel.agent.timeout-seconds` | 本地模型较慢时适当增加 |
| `travel.agent.stream` | 控制 Chat 模型内部生成方式，不代表 HTTP SSE |
| `travel.agent.state-directory` | 会话状态保存目录，需要隔离不同环境时修改 |
| `travel.agent.memory-workspace-directory` | 长期记忆工作区，默认 `.agentscope-data/travel-memory` |
| `travel.context.time-zone` | 动态时间提示使用的 IANA 时区，默认 `Asia/Shanghai` |
| `travel.rag.knowledge-pattern` | Markdown 初始化素材的匹配规则 |
| `travel.rag.top-k` | 默认返回片段数量，范围 1 到 10 |
| `travel.rag.embedding.base-url` | Embedding 服务地址，可通过 `EMBEDDING_BASE_URL` 覆盖 |
| `travel.rag.embedding.api-key` | 通过 `EMBEDDING_API_KEY` 注入；不要求认证的服务可为空 |
| `travel.rag.embedding.model-name` | Embedding 模型名称，可通过 `EMBEDDING_MODEL_NAME` 覆盖 |
| `travel.rag.embedding.dimension` | 向量维度，默认 1024，可通过 `EMBEDDING_DIMENSION` 覆盖 |
| `travel.rag.embedding.timeout-seconds` | Embedding HTTP 连接和读取超时 |
| `travel.rag.vector.url` | PostgreSQL JDBC 地址，可通过 `PG_URL` 覆盖 |
| `travel.rag.vector.username` | PostgreSQL 用户名，可通过 `PG_USER` 覆盖 |
| `travel.rag.vector.password` | 从必填的 `PG_PASSWORD` 环境变量读取 |
| `travel.rag.vector.min-similarity` | 最低余弦相似度，真实 Qwen3 模型默认 `0.45`；演示哈希需显式设为 `0.15` |
| `travel.rag.vector.candidate-limit` | pgvector 候选上限，当前为 `20` |
| `spring.datasource.url` | MySQL 业务库地址 |
| `spring.datasource.username` | MySQL 用户名，可通过 `MYSQL_USER` 覆盖 |
| `spring.datasource.password` | 从必填的 `MYSQL_PASSWORD` 环境变量读取 |
| `spring.jpa.hibernate.ddl-auto` | 当前为 `update`，仅建议本地学习使用 |

`TravelAgentProperties`、`TravelRagProperties`、`TravelEmbeddingProperties`、`TravelVectorProperties` 和 `TravelContextProperties` 中的字段默认值与 YAML 配置共同决定最终参数。

### 切换 Embedding 模型与迁移向量表

默认真实实现是 `OpenAiCompatibleEmbeddingService`，使用 `@Profile("!demo-embedding")`；256 维 `DemoFeatureHashEmbeddingService` 使用 `@Profile("demo-embedding")`。两者互斥，每个环境只能有一个 `TravelEmbeddingService` Bean。真实模型标记由“模型名称 + @ + 维度”组成，当前为 `Qwen3-Embedding-0.6B@1024`。

切换模型时按以下步骤操作：

1. **核对服务**：通过 `EMBEDDING_BASE_URL`、`EMBEDDING_MODEL_NAME`、`EMBEDDING_DIMENSION` 和可选的 `EMBEDDING_API_KEY` 指向新服务，不把真实密钥写入 YAML。
2. **区分文档与查询**：知识同步调用 `embedDocument`，查询调用 `embedQuery`。当前 Qwen3 查询会添加检索指令，二者必须使用同一模型、版本和维度。
3. **停止写入**：停止应用，确认 `PG_URL` 指向正确数据库，记录当前表数量、模型标记和维度。
4. **保留旧表**：`CREATE TABLE IF NOT EXISTS` 不会修改已有 `VECTOR(256)`。优先重命名旧表及其索引，而不是直接 `DROP TABLE`。
5. **重建并验证**：重新启动应用，让它按新维度自动建表并生成向量；核对数量、维度、模型标记、索引、幂等和检索结果。

2026-11-27 已按以下方式完成 256→1024 迁移。旧表名带日期，重复操作前必须先检查目标备份表是否已存在：

```sql
BEGIN;

ALTER TABLE travel_knowledge_embedding
RENAME TO travel_knowledge_embedding_demo256_backup_20261127;

ALTER TABLE travel_knowledge_embedding_demo256_backup_20261127
RENAME CONSTRAINT travel_knowledge_embedding_pkey
TO travel_knowledge_embedding_demo256_backup_20261127_pkey;

ALTER INDEX idx_travel_knowledge_embedding_model
RENAME TO travel_knowledge_embedding_demo256_backup_20261127_model_idx;

ALTER INDEX idx_travel_knowledge_embedding_hnsw
RENAME TO travel_knowledge_embedding_demo256_backup_20261127_hnsw_idx;

COMMIT;
```

随后应用自动创建新的 `travel_knowledge_embedding` 表、`VECTOR(1024)` 列和正式索引，并从 MySQL 生成 19 条真实向量。核对结果：

```sql
SELECT COUNT(*) AS vector_count,
       COUNT(DISTINCT chunk_id) AS unique_chunks,
       MIN(vector_dims(embedding)) AS min_dimension,
       MAX(vector_dims(embedding)) AS max_dimension
FROM travel_knowledge_embedding;

SELECT COUNT(*) AS backup_count,
       MIN(vector_dims(embedding)) AS backup_dimension
FROM travel_knowledge_embedding_demo256_backup_20261127;
```

当前正式表应为 19 条唯一片段、最小和最大维度均为 1024；备份表应保留 19 条、256 维。MySQL `travel_knowledge_chunk` 不受影响，不需要删除。

如需回滚，先停止应用并确认新表数据已完整生成。下面通过再次重命名保留 1024 新表，避免直接删除：

```sql
BEGIN;

ALTER TABLE travel_knowledge_embedding
RENAME CONSTRAINT travel_knowledge_embedding_pkey
TO travel_knowledge_embedding_qwen1024_rollback_20261127_pkey;

ALTER INDEX idx_travel_knowledge_embedding_model
RENAME TO idx_travel_knowledge_embedding_qwen1024_rollback_model;

ALTER INDEX idx_travel_knowledge_embedding_hnsw
RENAME TO idx_travel_knowledge_embedding_qwen1024_rollback_hnsw;

ALTER TABLE travel_knowledge_embedding
RENAME TO travel_knowledge_embedding_qwen1024_rollback_20261127;

ALTER TABLE travel_knowledge_embedding_demo256_backup_20261127
RENAME TO travel_knowledge_embedding;

ALTER TABLE travel_knowledge_embedding
RENAME CONSTRAINT travel_knowledge_embedding_demo256_backup_20261127_pkey
TO travel_knowledge_embedding_pkey;

ALTER INDEX travel_knowledge_embedding_demo256_backup_20261127_model_idx
RENAME TO idx_travel_knowledge_embedding_model;

ALTER INDEX travel_knowledge_embedding_demo256_backup_20261127_hnsw_idx
RENAME TO idx_travel_knowledge_embedding_hnsw;

COMMIT;
```

回滚后使用 `SPRING_PROFILES_ACTIVE=demo-embedding`，并显式设置 `--travel.rag.vector.min-similarity=0.15`。演示 Profile 必须连接独立的 256 维演示数据库，不能指向当前 1024 维正式库。应用只创建不存在的表和索引，不提供自动删表或自动迁移向量维度的功能；启动时检测到维度不一致会明确失败。

## 6. 会话、长期记忆与隔离调试

每次请求都需要传入 `sessionId`：

```json
{
  "sessionId": "debug-001",
  "message": "继续刚才的成都行程"
}
```

会话状态和长期记忆是两套机制：

| 机制 | 保存位置 | 生命周期 |
| --- | --- | --- |
| Agent 会话状态 | `.agentscope-data/travel` | 同一 `sessionId` 内保留路由、专家和指南上下文 |
| 每日记忆日志 | `.agentscope-data/travel-memory/<userId>/memory/YYYY-MM-DD.md` | Guide 调用结束后异步追加 |
| 整理后的长期记忆 | `.agentscope-data/travel-memory/<userId>/MEMORY.md` | LLM 定期合并、去重，同一用户跨会话共享 |

排查上下文问题时：

1. 多轮请求必须使用同一个 `sessionId`。
2. 检查 `.agentscope-data/travel` 是否产生状态文件。
3. 区分路由、专家和指南各自的会话上下文。
4. 如果切换了专家类型，专家会话也会变化，这是当前设计。
5. 跨会话验证偏好时，第一次请求返回后要等待或轮询每日日志和 `MEMORY.md`；记忆 hooks 是异步的，不能立即断言。
6. 如果首次只生成每日日志，检查 `Memory repair consolidation failed` 或等待超时日志；当前服务会在后台任务静默后按水印补偿整理。
7. 检查每日日志是否只包含 `Memory Flush` 段落；当前已移除 `memory_save`，不应出现 `Memory Save` 重复写入。
8. 同一用户不同 `sessionId` 应看到同一份 `MEMORY.md`；不同 `userId` 应位于不同目录。
9. 需要重新开始时使用新的 `sessionId`，或清理对应本地状态；清理长期记忆会影响该用户所有会话，需谨慎操作。

当前 HTTP 用户 ID 固定为 `local-demo-user`，不要把它当作完整的用户认证方案。工作区已经支持按 `userId` 隔离，但正式接入时必须从认证上下文获取用户 ID，不能接受请求体随意传入的身份。完整说明见 [长期记忆学习指南](04-long-term-memory-guide.md)。

### 单次运行时上下文调试

`TravelRuntimeContextFactoryTest` 和 `TravelRuntimeContextMiddlewareTest` 覆盖以下边界：

- 同一次请求的 Router、专家和 Guide 共享同一个 `TravelRequestContext`。
- 每次请求生成不同的 `requestId`，不会把上次请求属性带到本次调用。
- 业务 `sessionId` 与 Agent 会话 ID 分开，六个 Agent 的状态仍按各自命名空间保存。
- 动态提醒包含当前时间、时区和渠道，不包含 `requestId`。
- 提醒消息标记 `METADATA_SYNTHETIC=true`，只进入当前 `ReasoningInput`，原始消息列表保持不变。
- 缺少旅游请求元数据时中间件直接委托，不强行注入空提示。

需要打开链路日志时使用 debug 级别；日志中的 `requestId`、`agentSessionId` 和 `agent` 可用于定位调用阶段。不要为了调试把 `requestId`、内部路径或完整运行时对象加入模型提示。

## 7. 接入实时工具

当前 Agent 只保留记忆读取与会话召回工具，不提供天气、地图、搜索等外部工具。长期记忆由 Guide 的自动 hooks 写入，所有 Agent 都没有 `memory_save` 工具。

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
- Chat 和 Embedding 的 `base-url` 是否都可访问。
- Chat 的 `model-name`、Embedding 的 `model-name` 和 `dimension` 是否与模型列表及实际输出一致。
- `OPENAI_API_KEY` 是否满足 Chat 服务要求；本机 omlx 还需要提供 `EMBEDDING_API_KEY`。
- 配置中的温度和 Token 数是否在校验范围内。
- MySQL 是否已创建目标数据库，`MYSQL_PASSWORD` 是否已设置且账号允许连接。
- `PG_URL`、`PG_USER` 是否指向正确数据库，必填的 `PG_PASSWORD` 是否已设置。
- PostgreSQL 服务器是否已安装 pgvector 0.8.2，账号是否有创建扩展、表和索引的权限。
- MySQL `travel_knowledge_chunk` 是否成功创建并加载 19 个片段。
- PostgreSQL `travel_knowledge_embedding` 是否存在，向量维度、模型标记和索引是否符合当前 Embedding 实现。
- 若从演示 Profile 切换回真实模型，确认 `PG_URL` 指向 1024 维正式库，而不是 256 维演示库。

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

### 旅游问答没有使用知识

先调用 `/api/travel/knowledge/search` 确认是否命中，再检查 `travel.rag.vector.min-similarity`、`travel.rag.vector.candidate-limit` 和 `travel.rag.top-k`。同时核对 MySQL 片段数量、PostgreSQL 当前模型的向量数量与维度，具体 SQL 见 [RAG 学习指南](03-rag-learning-guide.md#9-核对-mysql-与-pgvector-数据)。真实 Qwen3 模型的默认阈值 `0.45` 已按美食、预算、安全和无关查询校准；如果仍然误命中，先检查知识库是否缺少对应主题，不要只靠继续调阈值。问答接口不会返回独立检索的原始过程，但响应的 `knowledgeSources` 可以说明本次检索到并交给 Agent 的片段。

### 答案缺少知识引用

查看 `knowledgeSources` 是否为空。来源非空时，检查 `TravelGuideService` 传给专家和指南的引用要求，以及 `TravelAgentPrompts.GUIDE` 是否要求保留编号。末尾的知识来源章节由程序追加，不依赖模型生成。

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

- [ ] Maven 干净打包成功，39 个单元测试全部通过。
- [ ] `travel.context.time-zone` 使用有效 IANA 时区，空值时启动校验失败。
- [ ] 本地 Chat 和 Embedding 模型列表都可以访问，所需密钥只通过环境变量提供。
- [ ] MySQL `travel_knowledge_chunk` 中有 19 个知识片段。
- [ ] PostgreSQL `travel_knowledge_embedding` 中有 19 条唯一 `chunk_id`，均为 1024 维 `Qwen3-Embedding-0.6B@1024`，普通索引和 HNSW 索引存在。
- [ ] `travel_knowledge_embedding_demo256_backup_20261127` 保留 19 条 256 维演示向量。
- [ ] 重复启动时向量同步数量为 0。
- [ ] 真实模型美食、预算和安全查询命中预期来源，感冒、红烧肉和股票等无关查询返回空结果。
- [ ] 独立检索已知问题返回非空结果，`score` 在 0–100 之间。
- [ ] 独立检索未知问题返回空 `results` 和 `totalMatches: 0`。
- [ ] 行程问题路由到 `ITINERARY`。
- [ ] 美食问题路由到 `FOOD`。
- [ ] 预算问题路由到 `BUDGET`。
- [ ] 不明确问题路由到 `GENERAL`。
- [ ] 同一 `sessionId` 可以保留上下文。
- [ ] Guide 自动生成每日记忆日志，且没有 `Memory Save` 重复段落。
- [ ] 异步整理或补偿整理生成 `MEMORY.md` 和 `.consolidation_state`。
- [ ] 同一用户不同 `sessionId` 可以召回同一偏好。
- [ ] 同一次问答的 Router、专家和 Guide 共享请求元数据，但 Agent 会话 ID 保持独立。
- [ ] 不同问答生成不同 `requestId`，动态时间提示不向模型暴露该标识。
- [ ] synthetic 运行时提醒不修改原始消息列表，不进入持久化状态或长期记忆。
- [ ] 描述长期记忆的句子不添加 RAG 引用编号。
- [ ] 不同 `userId` 的记忆目录和真实模型回答互相隔离。
- [ ] 非法 `sessionId` 返回 HTTP 400。
- [ ] 响应包含 `notice` 实时信息提醒。
- [ ] 命中知识的问答返回非空 `knowledgeSources`，答案末尾包含知识来源。
- [ ] 未命中知识的问答返回空 `knowledgeSources`，不生成虚假引用。
- [ ] 明文密钥没有提交到共享仓库。

## 10. 当前代码风格说明

项目运行环境是 JDK 21，但示例代码尽量采用熟悉 JDK 8 的开发者容易理解的写法：

- 公共数据模型使用普通 JavaBean，而不是 `record`。
- 使用显式字段、构造方法、Getter 和 Setter。
- 配置属性使用字段默认值，便于直接看到默认参数。
- 注释重点解释 Agent 职责、结构化输出、会话隔离和兜底逻辑。

目前 `TravelGuideService` 和 `TravelAgentPrompts` 中仍有文本块、`switch` 表达式等 JDK 21 写法，阅读时应结合 JDK 21 语法理解。
