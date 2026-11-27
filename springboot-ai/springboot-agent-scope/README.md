# Spring Boot + AgentScope 旅游多智能体示例

本项目使用 **AgentScope Java 2.0.4** 和 **Spring Boot 3.5.16**，通过本地 OpenAI 兼容模型实现一个旅游问答 Demo：

1. 客服路由 Agent 判断用户意图。
2. 应用只使用用户当前问题生成查询向量，从 PostgreSQL pgvector 取候选，再按 `chunk_id` 从 MySQL 批量读取知识正文。
3. 行程、美食、预算或通用专家 Agent 基于问题和检索材料生成专业材料。
4. 旅游指南 Agent 把专家材料整理为统一回复，并保留知识引用。

项目基于 JDK 21 构建，代码风格面向熟悉 JDK 8 的开发者，但当前依赖要求 JDK 21，不能直接使用 JDK 8 运行。

## 技术栈

| 技术 | 版本或用途 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| AgentScope Java | 2.0.4 |
| 模型接入 | OpenAI 兼容 Chat Completions API |
| 本地模型示例 | `Qwen3.5-4B-MLX-4bit` |
| Agent 状态存储 | `JsonFileAgentStateStore` |
| 长期记忆 | AgentScope Harness 两层记忆，本地文件按用户隔离 |
| 知识正文 | MySQL 8 + Spring Data JPA |
| 向量存储与检索 | PostgreSQL 17.9 + pgvector 0.8.2，应用层 JDBC |
| Embedding | `DemoFeatureHashEmbeddingService`，`demo-feature-hash-v3`，256 维；仅用于跑通链路 |
| 接口风格 | HTTP JSON，非 SSE 流式接口 |

> 本项目是 AgentScope Java 示例。`docs.agentscope.io/zh/versions/2.0.12` 对应 Python 版文档，Java API 应以 [AgentScope Java 2.0 文档](https://java.agentscope.io/v2) 为准。当前 RAG 明确采用应用层 JDBC + pgvector，不与 Python 文档中的存储方案混用。

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
│   ├── rag
│   ├── config
│   └── resources
│       └── knowledge/travel
└── docs
    ├── 01-architecture-and-flow.md
    ├── 02-development-guide.md
    ├── 03-rag-learning-guide.md
    └── 04-long-term-memory-guide.md
```

### API 模块

`springboot-agent-scope-api` 只保存旅游场景的公共数据模型：

- `AgentType`：专家类型枚举。
- `TravelChatRequest`：HTTP 请求参数。
- `TravelChatResponse`：HTTP 响应结果，包含本次使用的知识来源。
- `RouteDecision`：路由 Agent 的结构化输出。
- `KnowledgeSearchRequest`、`KnowledgeSearchResponse`、`KnowledgeSearchResult`：独立知识检索接口模型。

### Travel 模块

`springboot-agent-scope-travel` 负责实际运行：

- `TravelChatController`：提供 `/api/travel/chat`。
- `TravelKnowledgeController`：提供独立知识检索接口 `/api/travel/knowledge/search`。
- `TravelGuideService`：编排路由、知识检索、专家和指南 Agent，并追加知识来源。
- `TravelKnowledgeService`：同步 MySQL 知识片段，生成或更新 pgvector 向量，并编排候选筛选和正文回查。
- `TravelEmbeddingService`、`DemoFeatureHashEmbeddingService`：统一文本转向量接口及当前 256 维演示实现。
- `TravelKnowledgeChunk`、`TravelKnowledgeChunkRepository`：通过 JPA 持久化旅游知识正文。
- `TravelVectorRepository`：通过 JDBC 创建 pgvector 表和索引，幂等写入向量并执行余弦距离检索。
- `TravelAgentConfiguration`：创建模型、状态存储和六个 Agent。
- `TravelAgentPrompts`：集中管理六个 Agent 的提示词和旅游记忆抽取、整理规则。
- `TravelAgentProperties`、`TravelRagProperties`、`TravelVectorProperties`：绑定模型、超时、状态目录和 RAG 配置。
- `TravelVectorConfiguration`：创建独立于 MySQL JPA 数据源的 PostgreSQL `JdbcTemplate`。

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
        ├── 2. TravelKnowledgeService.search(message)
        │      ├── 用当前问题生成查询向量
        │      ├── pgvector 按余弦距离返回候选
        │      ├── 按相似度阈值过滤
        │      ├── MySQL findAllById 批量取回正文
        │      ├── 按向量顺序输出，每来源最多 2 个
        │      └── 截取 Top-K
        │
        ├── 3. specialists[targetAgent]
        │      ITINERARY / FOOD / BUDGET / GENERAL
        │      基于带编号的知识材料回答
        │
        └── 4. travel-guide
               整理专家材料并保留引用
        │
        ▼
TravelChatResponse
```

六个 Agent 共享同一个模型实例和状态存储，但使用不同的 Agent 名称、提示词和会话命名空间。Guide 额外开启长期记忆自动抽取和整理，路由与专家只读取记忆；工作区按 `userId` 隔离，同一用户跨会话共享 `MEMORY.md` 和每日日志。

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
| `travel.agent.api-key` | 本地服务密钥，通过 `OPENAI_API_KEY` 环境变量注入 |
| `travel.agent.base-url` | OpenAI 兼容接口地址 |
| `travel.agent.model-name` | Chat 模型名称 |
| `travel.agent.stream` | 模型内部是否流式生成 |
| `travel.agent.temperature` | 生成随机程度 |
| `travel.agent.max-tokens` | 单次回答最大 Token 数 |
| `travel.agent.timeout-seconds` | 单次 Agent 调用超时时间 |
| `travel.agent.state-directory` | 会话状态保存目录 |
| `travel.agent.memory-workspace-directory` | 长期记忆工作区，默认 `.agentscope-data/travel-memory` |
| `travel.rag.knowledge-pattern` | 旅游知识文件匹配规则 |
| `travel.rag.top-k` | 每次问答默认检索的片段数量 |
| `travel.rag.vector.min-similarity` | 最低余弦相似度，当前为 `0.15`，范围 0 到 1 |
| `travel.rag.vector.candidate-limit` | 每次先从 pgvector 获取的候选数量，当前为 `20` |
| `travel.rag.vector.url` | PostgreSQL JDBC 地址，可通过 `PG_URL` 覆盖 |
| `travel.rag.vector.username` | PostgreSQL 用户名，可通过 `PG_USER` 覆盖 |
| `travel.rag.vector.password` | PostgreSQL 密码，必须通过 `PG_PASSWORD` 提供 |
| `spring.datasource.url` | MySQL 业务库连接地址 |
| `spring.datasource.username` | MySQL 用户名，可通过 `MYSQL_USER` 覆盖 |
| `spring.datasource.password` | MySQL 密码，必须通过 `MYSQL_PASSWORD` 环境变量提供 |
| `spring.jpa.hibernate.ddl-auto` | 当前为 `update`，仅用于本地学习环境自动维护 MySQL 表结构 |

当前示例配置用于本地学习。模型密钥、MySQL 密码和 PostgreSQL 密码均通过环境变量注入，不要将真实密钥提交到共享仓库。

### 3. 构建

在仓库根目录执行，使用 JDK 21：

```bash
mvn -pl springboot-ai/springboot-agent-scope/springboot-agent-scope-travel -am clean package
```

### 4. 准备 MySQL

当前运行依赖 MySQL 8 业务库。先在 MySQL 中创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS travel_rag
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

数据库名和用户名可通过 `MYSQL_DATABASE`、`MYSQL_USER` 覆盖；MySQL 主机地址通过 `spring.datasource.url` 配置或启动参数覆盖，密码只通过 `MYSQL_PASSWORD` 环境变量提供。

Hibernate 会在首次启动时创建 `travel_knowledge_chunk` 表。应用启动后把类路径中的 Markdown 素材同步到该表。重复启动会按片段 ID 更新已有内容，不会重复插入。

### 5. 准备 PostgreSQL 与 pgvector

准备 PostgreSQL 17.9、pgvector 0.8.2 和已存在的目标数据库。应用首次启动时，`TravelVectorRepository` 会自动执行：

1. `CREATE EXTENSION IF NOT EXISTS vector`。
2. `CREATE TABLE IF NOT EXISTS travel_knowledge_embedding`，保存 `chunk_id`、`embedding`、`model_name`、`content_hash` 和 `embedded_at`。
3. 创建 `model_name` 普通索引 `idx_travel_knowledge_embedding_model`。
4. 创建 HNSW 索引 `idx_travel_knowledge_embedding_hnsw`，使用 `vector_cosine_ops`。

PostgreSQL 账号需要能连接目标数据库，并具备创建扩展、建表、建索引及后续读写向量表的权限。`CREATE EXTENSION` 通常需要数据库所有者或被授予相应权限的账号；是否还需要超级用户，取决于服务器的 pgvector 安装方式。启动时应用会核对现有向量列维度与当前 Embedding 维度，不一致时明确终止启动，避免误写；应用不会自动删除或重建已有向量表，安全重建步骤见 [开发与扩展指南](docs/02-development-guide.md#替换为真实-embedding-模型)。

### 6. 启动

以下值均为占位符，请替换为本机或部署环境自己的配置，不要把真实密码写入命令历史或文档：

```bash
export MYSQL_DATABASE='<mysql-database>'
export MYSQL_USER='<mysql-user>'
export MYSQL_PASSWORD='<mysql-password>'
export PG_URL='jdbc:postgresql://<pg-host>:5432/<pg-database>'
export PG_USER='<pg-user>'
export PG_PASSWORD='<pg-password>'
export OPENAI_API_KEY='<model-api-key>'
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar
```

jar 名称已按模块 POM 核对：`artifactId` 为 `springboot-agent-scope-travel`，父 POM 版本为 `1.0`。`PG_PASSWORD` 为必填项，缺失或空白时配置校验会阻止应用启动。

默认端口为 `8080`。需要切换端口时：

```bash
java -jar springboot-ai/springboot-agent-scope/springboot-agent-scope-travel/target/springboot-agent-scope-travel-1.0.jar --server.port=18080
```

## 双数据库职责

| 数据库 | 当前职责 | 使用方式 |
| --- | --- | --- |
| MySQL 8 | `travel_knowledge_chunk` 保存 19 个知识片段及后续业务数据 | Spring Data JPA；启动时同步 Markdown，检索时按 ID 批量读取正文 |
| PostgreSQL 17.9 + pgvector 0.8.2 | `travel_knowledge_embedding` 保存 19 条向量，通过 `chunk_id` 关联 MySQL 片段 | 独立 `JdbcTemplate`；应用层建表、幂等 upsert 和余弦距离检索 |

当前 Embedding 实现是 `DemoFeatureHashEmbeddingService`，模型标记为 `demo-feature-hash-v3`。它先把文本转小写并去掉“推荐、当地、怎么、如何”等泛化短语，再用 SHA-256 特征哈希提取中文二元词和英文词，生成 256 维向量并做 L2 归一化，只用于验证 pgvector 链路，不具备真正的语义理解，同义改写或跨语言问题可能表现很差。

启动同步时，应用对每个片段的“标题 + 正文”计算 `content_hash`，再按 `chunk_id + content_hash + model_name` 判断是否最新；缺失或变化时生成向量并通过 `ON CONFLICT (chunk_id)` 幂等更新。检索时只使用用户当前问题：query embed → pgvector 候选 → 相似度阈值过滤 → MySQL `findAllById` 批量取正文 → 按向量顺序输出 → 每来源最多 2 个 → Top-K。SQL 使用 `embedding <=> CAST(? AS vector)` 获取余弦距离，应用层按 `similarity = 1 - distance` 计算相似度，并将分数限制在 0–100。

当前 25 个单元测试全部通过；真实库中存在 19 条唯一 `chunk_id`、均为 256 维的 `demo-feature-hash-v3` 向量，普通索引和 HNSW 索引已建立，重复启动的向量同步数量为 0。已知美食和预算查询能返回知识，未知问题返回空结果；真实 Chat 模型的美食问答也保留了 `[1]`、`[2]` 引用并返回两条 `knowledgeSources`。数据库核对方法见 [RAG 学习指南](docs/03-rag-learning-guide.md#9-核对-mysql-与-pgvector-数据)。

## RAG 知识检索与引用

项目提供两种使用方式：

1. `/api/travel/chat`：路由后自动检索知识，专家和指南 Agent 会收到带编号的材料。
2. `/api/travel/knowledge/search`：独立调试检索质量，不调用模型。

独立检索请求（不调用 Chat 模型，但需要 MySQL 和 PostgreSQL 均可用）：

```bash
curl -X POST http://localhost:8080/api/travel/knowledge/search \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "成都三天两夜有什么美食推荐",
    "topK": 3
  }'
```

旅游问答的 `answer` 会保留模型生成的 `[1]`、`[2]` 引用，并由程序统一追加“知识来源”章节；响应的 `knowledgeSources` 字段按相同顺序返回来源文件、标题和片段 ID。没有知识命中时，`knowledgeSources` 为空数组，提示词会要求专家明确说明未命中。

详细加载、向量同步、相似度计算和数据库核对方法见 [RAG 学习指南](docs/03-rag-learning-guide.md)。

## 长期记忆

项目使用 AgentScope Java 2.0.4 的 `HarnessAgent` 实现两层长期记忆：

- `memory/YYYY-MM-DD.md`：Guide 每次调用结束后异步追加的每日日志。
- `MEMORY.md`：LLM 定期合并、去重后的跨会话长期记忆。
- `memory/.consolidation_state`：最近一次成功整理的时间水印。

默认文件位于 `.agentscope-data/travel-memory/<userId>/`。同一用户跨 `sessionId` 共享记忆，不同用户目录隔离。路由和专家只读取记忆，只有 Guide 开启自动写入；所有 Agent 都移除了 `memory_save`，避免重复保存。

记忆写入是异步的，当前请求不会等待文件落盘。Harness 首次抽取与维护可能并发，`TravelGuideService` 会等待后台任务静默后按水印补偿整理，避免漏生成 `MEMORY.md`。旅游提示词只保留用户明确表达的偏好、约束、历史决策和必要事实，当前输入优先于历史记忆，敏感信息不进入日志。服务会把长期记忆作为独立段落提供给专家和指南 Agent；长期记忆不是 RAG 公共知识，描述记忆的句子不得添加检索引用编号。

配置、验证和排障步骤见 [长期记忆学习指南](docs/04-long-term-memory-guide.md)。

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
  "answer": "这里返回整理后的 Markdown 旅游指南，相关事实后可带 [1] 引用，末尾由程序追加知识来源。",
  "notice": "本指南由本地模型生成，未实时查询交通、票价、天气或营业信息，出行前请通过官方渠道核实。",
  "knowledgeSources": [
    {
      "id": "chengdu-food-1",
      "title": "成都美食指南 - 代表性美食",
      "source": "chengdu-food.md",
      "score": 87,
      "content": "这里返回实际检索到的知识正文。"
    }
  ]
}
```

`knowledgeSources` 的顺序与答案中的引用编号一致；没有命中时返回空数组。`score` 先将 `1 - distance` 的相似度限制在 0–1，再四舍五入为相似度百分比，范围为 0–100，不是概率，也不代表知识一定正确；示例分数仅用于展示格式，实际结果取决于查询和 Embedding 实现。`routedAgent` 可能为：

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

旅游问答：

- `sessionId` 不能为空，最长 100 个字符。
- `sessionId` 只能包含字母、数字、下划线和短横线。
- `message` 不能为空，最长 2000 个字符。

独立知识检索：

- `query` 不能为空，最长 500 个字符。
- `topK` 范围为 1 到 10，默认 3。

不符合规则时返回 HTTP 400。

## 推荐代码阅读顺序

1. `AgentType`：先理解四种专家类型。
2. `TravelChatRequest`、`TravelChatResponse`：理解接口契约。
3. `TravelAgentPrompts`：理解每个 Agent 的职责边界。
4. `TravelAgentConfiguration`：理解 Agent 是如何创建的。
5. `TravelKnowledgeService`：理解 MySQL 同步、pgvector 检索和正文回查。
6. `TravelEmbeddingService`、`DemoFeatureHashEmbeddingService`：理解文本转向量接口及演示算法。
7. `TravelVectorRepository`：理解自动建表、幂等写入和余弦距离查询。
8. `TravelGuideService`：理解检索材料如何进入多 Agent 编排，以及来源引用如何返回。
9. `TravelKnowledgeController`：理解独立检索接口。
10. `TravelChatController`：理解旅游问答 HTTP 入口。
11. `TravelAgentProperties`、`TravelRagProperties`、`TravelVectorProperties`：理解可调整的运行参数。

## 当前边界

- RAG 已接入专家和指南 Agent，使用应用层 JDBC + pgvector，但只检索项目内置静态知识，不是实时搜索。
- 知识正文由 MySQL + JPA 持久化，向量由 PostgreSQL 保存，两库通过 `chunk_id` 在应用层关联，没有跨库外键。
- 当前 256 维 Embedding 是特征哈希演示实现，不具备真正的语义理解；替换步骤见 [开发与扩展指南](docs/02-development-guide.md#替换为真实-embedding-模型)。
- 检索由应用层执行，不是 Agent 自主调用 RAG 工具，也没有混用 AgentScope Java 的内存 Store 或 Python 文档方案。
- 没有接入天气、地图、票价、搜索等实时工具。
- 工作区支持按 `userId` 隔离，但 HTTP 服务固定使用 `local-demo-user`，尚未接入真实登录和认证上下文。
- 模型配置支持流式生成，但 HTTP 接口仍返回聚合后的 JSON。
- 25 个单元测试已通过；真实数据库验证了 19 条唯一 256 维向量和两类索引，未知问题返回空结果。真实 Chat 模型已联调验证美食问答的引用与来源返回、同一用户跨会话的长期记忆召回、首次补偿整理和不同 `userId` 的记忆隔离，但本地小模型的生成质量仍有限。
- 本地小模型可能改变用户给出的天数、人数或生成不准确信息，回复中会附带核实提醒。

## 延伸阅读

- [架构与调用流程](docs/01-architecture-and-flow.md)
- [开发与扩展指南](docs/02-development-guide.md)
- [RAG 旅游知识检索学习指南](docs/03-rag-learning-guide.md)
- [长期记忆学习指南](docs/04-long-term-memory-guide.md)
- [AgentScope Java 2.0 官方文档](https://java.agentscope.io/v2)
