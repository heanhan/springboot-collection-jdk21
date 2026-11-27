# RAG 旅游知识检索学习指南

本文对应旅游多智能体项目已经完成的 RAG 链路：MySQL 保存知识正文，PostgreSQL + pgvector 保存向量，应用层完成 Embedding、候选检索、正文回查和来源引用。

当前知识正文通过 **MySQL 8 + Spring Data JPA** 持久化，共 22 个 `GLOBAL` 通用片段；**PostgreSQL 17.9 + pgvector 0.8.2** 保存对应的 22 条向量，通过 `chunk_id` 关联。当前默认使用 `Qwen3-Embedding-0.6B@1024` 真实 Embedding；旧的 256 维特征哈希实现仅在 `demo-embedding` Profile 下保留。

## 1. 什么是 RAG

RAG 是 Retrieval-Augmented Generation 的缩写：

1. **Retrieval（检索）**：根据用户问题，从知识库找到相关片段。
2. **Augmented（增强）**：把检索片段作为额外上下文放入提示词。
3. **Generation（生成）**：模型基于问题和检索资料生成回答。

当前实现采用应用层 RAG：Java 服务先从 pgvector 找到候选，再从 MySQL 取回正文，最后把材料放进 `HarnessAgent` 的输入，而不是让 Agent 自主调用向量检索工具。项目明确选择 **应用层 JDBC + pgvector**，不使用 AgentScope Java 的内存 Store 作为当前存储，也不混用 Python 文档方案。

## 2. 当前检索架构

### 启动同步

```text
src/main/resources/knowledge/travel/*.md
                │
                ▼
      TravelKnowledgeService
                │
                ├── 分块并按稳定 ID 同步到 MySQL
                │      travel_knowledge_chunk（22 个 GLOBAL 正文片段）
                │
                ├── 读取“标题 + 正文”，计算 content_hash
                ├── 按 chunk_id + content_hash + model_name 判断是否最新
                │
                ▼
   OpenAiCompatibleEmbeddingService（Qwen3-Embedding-0.6B@1024）
                │
                ▼
      TravelVectorRepository 幂等 upsert
                │
                ▼
PostgreSQL travel_knowledge_embedding（22 条向量）
```

### 查询链路

```text
用户当前问题
    │
    ▼
同一个 TravelEmbeddingService 生成查询向量
    │
    ▼
pgvector：embedding <=> CAST(? AS vector)，按余弦距离取候选
    │
    ▼
应用层 similarity = 1 - distance，按 min-similarity 过滤
    │
    ▼
MySQL findAllById，按 chunk_id 批量取回正文
    │
    ▼
保持向量顺序 → 每来源最多 2 个 → Top-K
    │
    ├── POST /api/travel/knowledge/search → KnowledgeSearchResponse
    │
    └── TravelGuideService → 专家 Agent → 指南 Agent → knowledgeSources
```

| 存储 | 职责 | 访问方式 |
| --- | --- | --- |
| MySQL `travel_knowledge_chunk` | 保存标题、来源、`GLOBAL` 范围、正文和时间戳 | Spring Data JPA |
| PostgreSQL `travel_knowledge_embedding` | 保存向量、`chunk_id`、模型标记、内容哈希和嵌入时间 | 独立 `JdbcTemplate` + pgvector |

两库没有跨库外键，由应用层通过 `chunk_id` 关联；MySQL 正文缺失的候选会被跳过。当前 40 个单元测试全部通过，真实库已验证 22 条唯一 1024 维 `Qwen3-Embedding-0.6B@1024` 向量、普通索引和 HNSW 索引，重复启动同步数量为 0。美食、预算和安全查询能返回预期通用知识，成都和上海等城市问题不会返回已下线的成都测试来源，无关问题返回空结果。

## 3. 知识文件

当前示例知识位于：

`springboot-agent-scope-travel/src/main/resources/knowledge/travel/`

包含：

- `global-itinerary-principles.md`：通用行程目标、节奏、区域组织和备选方案。
- `global-transport-accommodation.md`：通用城际交通、市内交通、住宿成本和入住核对。
- `global-food-guidance.md`：通用饮食节奏、食品卫生、过敏原和肠胃健康。
- `travel-budget.md`：通用预算结构、交通住宿、餐饮门票和弹性费用。
- `travel-safety.md`：通用信息核实、天气、证件保险、财物通讯和健康提醒。

这些是项目内置的静态通用知识，不包含城市专属事实，也不是实时天气、票价或政策数据。所有片段在 MySQL 中标记为 `GLOBAL`。

Markdown 文件是初始化素材，不是运行时直接读取的数据源。应用启动时把片段同步到 MySQL，再从数据库加载。后续可以直接通过 JPA 管理知识正文；如果仍保留 Markdown 素材，启动同步会以文件内容更新同 ID 的记录。启动同步只新增或更新片段，不会自动删除文件中已移除的数据库记录；退役知识必须先备份，再分别清理 MySQL 正文和 pgvector 向量。

## 4. 检索接口

### 请求

```http
POST /api/travel/knowledge/search
Content-Type: application/json
```

```json
{
  "query": "旅行期间如何安排饮食",
  "topK": 3
}
```

参数说明：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `query` | 是 | 检索问题，最长 500 个字符 |
| `topK` | 否 | 返回片段数量，范围 1 到 10，默认 3 |

### 响应

```json
{
  "query": "旅行期间如何安排饮食",
  "topK": 3,
  "totalMatches": 1,
  "results": [
    {
      "id": "global-food-guidance-1",
      "title": "通用旅行饮食指南 - 饮食节奏与预算",
      "source": "global-food-guidance.md",
      "score": 87,
      "content": "饮食节奏与预算\n保持规律三餐，按时吃饭，避免为了赶行程长时间空腹或暴饮暴食。"
    }
  ]
}
```

`score` 是相似度百分比：先计算 `similarity = 1 - distance`，再将相似度限制在 0–1，四舍五入为 `round(clamp(similarity, 0, 1) × 100)`，范围为 0–100。它不是概率，也不代表内容一定正确。示例分数仅用于展示格式，实际分数取决于查询和 Embedding 实现。

`totalMatches` 是阈值过滤、正文回查、每来源最多 2 个和 Top-K 处理后的实际返回数量；`topK` 是本次请求的最大返回数量。未命中时返回：

```json
{
  "query": "一个知识库完全不涉及的问题",
  "topK": 3,
  "totalMatches": 0,
  "results": []
}
```

### curl 示例

```bash
curl -X POST http://localhost:8080/api/travel/knowledge/search \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "旅行期间如何安排饮食",
    "topK": 3
  }'
```

## 5. Markdown 如何变成知识片段

`TravelKnowledgeService` 启动时读取配置匹配的知识文件，按以下规则生成片段，并通过 `TravelKnowledgeChunkRepository` 同步到 MySQL：

1. 一级标题作为文档标题。
2. 二级、三级标题作为片段标题。
3. 空行结束当前段落。
4. 每个非空段落生成一个知识片段。
5. 片段 ID 由文件名和序号组成，例如 `global-food-guidance-1`。
6. 来源保存为 Markdown 文件名，范围保存为 `GLOBAL`，便于回答时引用和后续过滤。
7. 根据 ID 查询数据库；不存在时插入，标题、来源、范围或正文变化时更新。
8. 从数据库读取全部片段，对“标题 + 换行 + 正文”计算 SHA-256 `content_hash`。
9. 按 `chunk_id + content_hash + model_name` 判断向量是否最新；缺失或变化时生成向量并幂等写入 pgvector。

`TravelKnowledgeChunk` 对应 MySQL `travel_knowledge_chunk` 表，包含 `id`、`title`、`source`、`scope`、`content`、`created_at` 和 `updated_at`。当前 `scope` 均为 `GLOBAL`，数据库列默认值也是 `GLOBAL`，便于已有通用记录平滑补充范围。同步时保留已有记录的创建时间，只更新内容变化的记录及其更新时间。

PostgreSQL `travel_knowledge_embedding` 保存 `chunk_id`、`embedding`、`model_name`、`content_hash` 和 `embedded_at`。当前模型标记为 `Qwen3-Embedding-0.6B@1024`，向量列为 `VECTOR(1024)`；`chunk_id` 是主键，`ON CONFLICT (chunk_id)` 保证重复启动和重复同步不会产生重复向量记录。旧的 256 维 `demo-feature-hash-v3` 数据保留在 `travel_knowledge_embedding_demo256_backup_20261127` 备份表中。

示例：

```markdown
# 通用旅行饮食指南

## 饮食节奏

保持规律三餐，按时吃饭，避免长时间空腹或暴饮暴食。
```

会生成一个标题为“通用旅行饮食指南 - 饮食节奏”的 `GLOBAL` 知识片段。

## 6. 真实 Embedding、相似度与阈值

默认实现是 `OpenAiCompatibleEmbeddingService`：

| 项目 | 当前值 |
| --- | --- |
| 模型 | `Qwen3-Embedding-0.6B` |
| 模型标记 | `Qwen3-Embedding-0.6B@1024` |
| 向量维度 | 1024 |
| 接口 | OpenAI 兼容 `POST {baseUrl}/embeddings` |
| 请求字段 | `model`、`input`、`dimensions` |
| 认证 | 服务要求认证时发送 `Authorization: Bearer ...` |
| Profile | `!demo-embedding` |

生成过程：

1. `embedDocument` 直接发送“标题 + 换行 + 正文”，不添加查询指令。
2. `embedQuery` 在用户当前问题前添加 Qwen3 检索指令：`Instruct: Given a web search query, retrieve relevant passages that answer the query`，随后拼接 `Query: {问题}`。
3. 适配层要求响应包含 `index=0`，向量非空、维度与配置一致、每个值都是有限浮点数，且不是零向量。
4. 对返回向量做 L2 归一化，保证不同文本可以使用统一的余弦距离比较。
5. 模型标记加入维度，避免模型升级或维度变化后继续复用无法区分的旧记录。

`DemoFeatureHashEmbeddingService` 是 256 维 SHA-256 特征哈希演示实现，只在 `demo-embedding` Profile 下启用。它没有经过语义模型训练，不参与默认正式检索。

检索过程：

1. 使用用户当前问题生成查询向量，不拼接路由理由。
2. `TravelVectorRepository` 只查询当前 `model_name`，执行 `embedding <=> CAST(? AS vector)`，按余弦距离升序取前 `candidate-limit` 个候选。
3. 应用层计算 `similarity = 1 - distance`，过滤低于 `min-similarity` 的候选。
4. 按候选 `chunk_id` 从 MySQL `findAllById` 批量取回正文，再按 pgvector 的向量顺序输出。
5. 同一来源最多保留 2 个片段，最后截取 Top-K；距离相同时按 `chunk_id` 升序。
6. `score` 先将相似度限制在 0–1，再计算 `round(similarity × 100)`，是 0–100 的相似度百分比，不是概率。

当前真实模型的候选上限为 20、最低相似度为 0.45。2026-11-27 使用通用正式语料完成以下验证：

| 查询 | 观察结果 |
| --- | --- |
| 成都三天两夜有什么美食推荐 | 返回饮食常识和安全通用知识，不返回成都专属来源 |
| 上海三天两夜行程 | 返回交通住宿和行程原则通用知识 |
| 三个人旅行五天怎么安排预算 | 返回预算和行程原则通用知识 |
| 旅行期间如何安排饮食 | 返回饮食常识和安全通用知识 |
| 量子纠缠实验 | 返回空结果 |

当前知识库只有 `GLOBAL` 通用知识，因此城市问题命中的是行程、交通、住宿、饮食、预算和安全方法，不代表已经具备该城市的本地知识。阈值不是跨模型通用常量：更换 Embedding 模型、维度或知识库后都要重新采样校准。演示哈希向量的分数尺度不同，在独立演示数据库中启用时应显式设置 `--travel.rag.vector.min-similarity=0.15`。

## 7. 配置项

当前配置直接写在 `springboot-agent-scope-travel/src/main/resources/application.yml`，不使用 `${ENV:default}` 占位符。本文不复制真实账号、密码和密钥，只说明可调整的非敏感参数。该配置文件包含本机实际凭据，提交到共享仓库前必须先脱敏。

| 配置 | 说明 |
| --- | --- |
| `travel.rag.knowledge-pattern` | 启动同步所需的 Spring Resource 匹配规则 |
| `travel.rag.top-k` | 默认返回片段数量，范围 1 到 10 |
| `travel.rag.embedding.base-url` | Embedding 服务地址，当前指向本地 OpenAI 兼容服务 |
| `travel.rag.embedding.api-key` | Embedding 服务密钥，按本机服务要求直接配置，不在文档中展示 |
| `travel.rag.embedding.model-name` | 当前模型为 `Qwen3-Embedding-0.6B` |
| `travel.rag.embedding.dimension` | 当前维度为 1024 |
| `travel.rag.embedding.timeout-seconds` | Embedding HTTP 连接和读取超时 |
| `travel.rag.embedding.query-instruction` | Qwen3 查询向量使用的检索指令 |
| `travel.rag.vector.url` | PostgreSQL JDBC 地址，直接配置实际值 |
| `travel.rag.vector.username` | PostgreSQL 用户名，直接配置实际值 |
| `travel.rag.vector.password` | PostgreSQL 密码，直接配置实际值，不在文档中展示 |
| `travel.rag.vector.min-similarity` | 最低余弦相似度，真实模型默认 `0.45`，演示模型显式使用 `0.15` |
| `travel.rag.vector.candidate-limit` | pgvector 候选上限，当前为 `20` |
| `spring.datasource.url` | MySQL 业务库 JDBC 地址，直接配置实际值 |
| `spring.datasource.username` | MySQL 用户名，直接配置实际值 |
| `spring.datasource.password` | MySQL 密码，直接配置实际值，不在文档中展示 |
| `spring.jpa.hibernate.ddl-auto` | 当前为 `update`，只建议本地学习环境使用 |

调整建议：

- 命中过多：提高 `min-similarity`、降低 `top-k`，或检查知识段落是否过于宽泛。
- 命中过少：先核对两库数量、模型标记和 Embedding 服务，再适当降低 `min-similarity`。
- 结果不相关：优先检查知识覆盖和正文质量。酒店等主题缺失时应补充知识，不要期待调阈值补出不存在的内容。
- 候选遗漏：适当增加 `candidate-limit`，但会增加 MySQL 回查和距离计算开销。
- 单次上下文过长：减少 `top-k` 或缩短知识段落。

## 8. 如何新增旅游知识

1. 在 `knowledge/travel` 目录新增 Markdown 文件。
2. 使用一个一级标题说明文档主题。
3. 使用二级标题拆分不同知识主题。
4. 每段只表达一个完整知识点。
5. 明确区分静态知识与实时信息。
6. 重新启动应用，使新知识同步到 MySQL，并生成或更新 pgvector 向量。
7. 查询两张表，确认正文和向量数量、模型标记及维度一致。
8. 调用独立检索接口验证命中结果。

应用首次启动时，`TravelVectorRepository` 会自动创建 pgvector 扩展、`travel_knowledge_embedding` 表、`model_name` 普通索引和 HNSW `vector_cosine_ops` 索引。前提是 PostgreSQL 服务器已安装 pgvector 0.8.2、目标数据库已存在，且运行账号具备创建扩展、表和索引以及读写向量表的权限。已有表不会被自动删除或改变维度；每次启动都会核对现有向量维度，若与当前 Embedding 不一致会明确失败，避免误写。

如果不再使用 Markdown 初始化，也可以通过 JPA Repository 直接维护 MySQL 知识；应用启动时会读取全部片段并同步缺失或变化的向量。当前启动同步不会删除 Markdown 中已经移除、但数据库仍存在的旧片段，也不会自动清理对应的旧向量，需要清理时应人工确认后同时处理两张表。

建议一个片段控制在几百字以内，避免一次返回过多内容。

## 9. 核对 MySQL 与 pgvector 数据

先连接与应用相同的两个数据库。以下命令只包含占位符，密码通过交互提示输入，不要写在命令行中：

```bash
mysql -h '<mysql-host>' -P 3306 -u '<mysql-user>' --password --database='<mysql-database>'
psql -h '<pg-host>' -p 5432 -U '<pg-user>' -d '<pg-database>' -W
```

### MySQL 核对

```sql
SELECT COUNT(*) AS chunk_count,
       COUNT(DISTINCT id) AS unique_chunks
FROM travel_knowledge_chunk;

SELECT scope, COUNT(*) AS chunk_count
FROM travel_knowledge_chunk
GROUP BY scope
ORDER BY scope;

SELECT id, title, source, scope, CHAR_LENGTH(content) AS content_length
FROM travel_knowledge_chunk
ORDER BY id;
```

当前基线应为 22 个片段、22 个唯一 ID，且 `scope` 全部为 `GLOBAL`。记下按 ID 排序的列表，用于和 pgvector 的 `chunk_id` 对比。

### PostgreSQL 核对

```sql
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';

SELECT COUNT(*) AS vector_count,
       COUNT(DISTINCT chunk_id) AS unique_chunks,
       MIN(vector_dims(embedding)) AS min_dimension,
       MAX(vector_dims(embedding)) AS max_dimension,
       COUNT(*) FILTER (
           WHERE vector_dims(embedding) <> 1024
       ) AS wrong_dimension_count
FROM travel_knowledge_embedding;

SELECT model_name, COUNT(*) AS vector_count
FROM travel_knowledge_embedding
GROUP BY model_name
ORDER BY model_name;

SELECT chunk_id
FROM travel_knowledge_embedding
WHERE model_name = 'Qwen3-Embedding-0.6B@1024'
ORDER BY chunk_id;

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
  AND tablename = 'travel_knowledge_embedding'
ORDER BY indexname;

SELECT COUNT(*) AS backup_count,
       MIN(vector_dims(embedding)) AS min_dimension,
       MAX(vector_dims(embedding)) AS max_dimension
FROM travel_knowledge_embedding_demo256_backup_20261127;

SELECT model_name, COUNT(*) AS backup_count
FROM travel_knowledge_embedding_demo256_backup_20261127
GROUP BY model_name;
```

当前正式表基线应为 22 条唯一 `chunk_id`，最小和最大维度均为 1024，错误维度数量为 0，模型标记为 `Qwen3-Embedding-0.6B@1024`。`chunk_id` 排序结果应与 MySQL 的 `id` 列表一致。备份表应保留 19 条、256 维、`demo-feature-hash-v3` 数据。正式表索引结果应包含：

- `travel_knowledge_embedding_pkey`：`chunk_id` 主键。
- `idx_travel_knowledge_embedding_model`：`model_name` 普通索引。
- `idx_travel_knowledge_embedding_hnsw`：HNSW 索引，方法为 `vector_cosine_ops`。

可以用一条已存储的向量核对余弦距离和排序，不需要手工拼写 1024 个浮点数：

```sql
WITH query_vector AS (
    SELECT embedding
    FROM travel_knowledge_embedding
    WHERE chunk_id = 'global-food-guidance-1'
      AND model_name = 'Qwen3-Embedding-0.6B@1024'
)
SELECT e.chunk_id,
       1 - (e.embedding <=> q.embedding) AS similarity
FROM travel_knowledge_embedding AS e
CROSS JOIN query_vector AS q
WHERE e.model_name = 'Qwen3-Embedding-0.6B@1024'
ORDER BY e.embedding <=> q.embedding, e.chunk_id
LIMIT 20;
```

`global-food-guidance-1` 与自身比较时相似度应为 1。这条 SQL 只用于核对数据库距离运算，实际 HTTP 检索仍会先通过 `embedQuery` 生成查询向量。

## 10. 如何调试

### 没有命中

- 查询 MySQL `travel_knowledge_chunk`，确认对应片段已经同步。
- 查询 PostgreSQL，确认当前 `model_name` 下有对应 `chunk_id`，且向量维度正确。
- 检查两张表的 ID 是否一致；MySQL 正文缺失的候选会被跳过。
- 检查 `min-similarity` 和 `candidate-limit`，必要时调整后重新调用独立检索接口。
- 查看启动日志中的 MySQL 片段数量和向量同步数量。
- 如果问题是语义相近但用词不同的表达仍无法命中，检查 Embedding 服务认证、模型名称、维度和 Qwen3 查询指令。
- 本机 omlx 缺少 `travel.rag.embedding.api-key` 时会导致启动同步失败，应先修正 YAML 配置，不要通过降低阈值绕过。

### 命中了错误知识

- 检查标题是否过于宽泛，段落是否混合多个主题。
- 拆分知识段落，使每个片段只表达一个完整知识点。
- 确认文档向量和查询向量使用同一模型、同一版本和同一维度。
- 如果知识库缺少对应主题，例如酒店区域，应补充知识；真实语义模型仍无法检索出不存在的内容。

### 分数很高但内容不相关

相似度百分比只表示当前向量空间中的距离，不是内容正确率。先核对数据库和模型标记，再检查知识内容与主题覆盖；不要仅凭高分认定答案可靠。阈值可以过滤明显无关结果，但不能修复错误或不完整的知识正文。

### 答案没有正文引用

先查看响应的 `knowledgeSources` 是否为空；为空说明没有知识命中。来源非空但正文没有 `[1]` 等标记时，检查专家和指南提示词中的引用要求。来源章节由程序追加，因此即使模型漏写正文引用，也不会丢失来源列表。

## 11. 当前实现边界

- 检索发生在应用层，使用 JDBC + pgvector，不是 Agent 自主调用工具。
- 知识正文持久化在 MySQL，向量持久化在 PostgreSQL，两库通过 `chunk_id` 关联。
- 默认使用真实 `Qwen3-Embedding-0.6B@1024`；256 维特征哈希仅保留在 `demo-embedding` Profile 中，用于独立演示数据库。
- 相似度阈值依赖模型和知识库，当前 0.45 只适用于已验证样本；酒店等未覆盖主题可能误命中其他旅游知识。
- Markdown 作为初始化素材，启动同步不会自动删除数据库中已移除的旧片段或旧向量。
- 应用首次启动会创建不存在的扩展、表和索引，但不会自动删表或迁移已有向量维度；维度不一致时会明确终止启动，要求人工备份和重建。
- `score` 是相似度百分比，不是概率，也不保证知识与用户条件一致。
- 没有权限过滤、租户隔离和知识版本管理。
- 返回内容来自静态资料，不保证实时准确。

## 12. Agent 如何消费检索结果

当前调用顺序：

```text
用户问题
  → travel-router
  → TravelKnowledgeService.search
  → 拼接带编号的检索证据
  → 对应专家 Agent
  → travel-guide
  → 统一追加来源并返回响应
```

### 检索查询如何组装

服务只使用用户当前问题检索，不拼接路由理由。路由理由可能包含“不属于旅游、美食或预算”等否定描述，拼入查询会造成误命中。多轮追问缺少上下文时，优先结合专家会话历史理解需求，不为了补全查询而牺牲检索准确性。

### 知识如何进入提示词

服务按检索结果顺序生成材料，例如：

```text
[1] 通用旅行饮食指南 - 饮食节奏与预算
来源文件：global-food-guidance.md
片段编号：global-food-guidance-1
保持规律三餐，按时吃饭，避免为了赶行程长时间空腹或暴饮暴食。

[2] 旅行安全与出行提醒 - 健康与饮食
来源文件：travel-safety.md
片段编号：travel-safety-5
准备常用药品并注意保质期，异地饮食应适量，持续或严重症状应及时就医。
```

专家 Agent 收到要求：优先依据知识回答，并在相关事实后保留 `[1]`、`[2]` 编号；没有命中时明确说明，不能把模型常识说成知识库内容。指南 Agent 同时收到专家材料和检索知识，并被要求保留这些编号。

### 长期记忆与 RAG 的边界

RAG 材料来自项目公共旅游知识库，使用 `[1]`、`[2]` 等编号标注；长期记忆来自当前用户过去明确表达的信息，例如舒缓节奏、住宿偏好和旅行约束，不属于检索知识，也不能添加知识引用编号。服务会把 `MEMORY.md` 作为独立段落提供给专家和指南 Agent，并要求描述记忆的句子禁止出现引用编号，混合内容必须拆句。

当前用户消息中的明确条件优先于历史记忆。模型不得用旧偏好改写用户本轮给出的目的地、天数、人数或预算，也不得把专家攻略、模型假设或 RAG 内容写入用户长期记忆。两层记忆的配置和验证方法见 [长期记忆学习指南](04-long-term-memory-guide.md)。

### 最终响应如何返回来源

`/api/travel/chat` 的响应新增 `knowledgeSources`：

```json
{
  "sessionId": "demo-001",
  "routedAgent": "FOOD",
  "routeReason": "用户想了解旅行期间的饮食安排。",
  "answer": "旅行期间应规律三餐、按时吃饭，并注意食品卫生。[1]\n\n### 知识来源\n- [1] `global-food-guidance.md` — 通用旅行饮食指南 - 饮食节奏与预算（片段 ID：`global-food-guidance-1`）",
  "notice": "本指南由本地模型生成，未实时查询交通、票价、天气或营业信息，出行前请通过官方渠道核实。",
  "knowledgeSources": [
    {
      "id": "global-food-guidance-1",
      "title": "通用旅行饮食指南 - 饮食节奏与预算",
      "source": "global-food-guidance.md",
      "score": 87,
      "content": "保持规律三餐，按时吃饭，避免为了赶行程长时间空腹或暴饮暴食。"
    }
  ]
}
```

知识来源章节由程序统一追加，不依赖模型自觉生成；`knowledgeSources` 的顺序与 `[1]`、`[2]` 编号一致，其中的 `score` 同为 0–100 相似度百分比。没有命中时返回空数组，且答案不包含知识来源章节。指南 Agent 返回空内容时，服务仍会使用专家回答并保留来源。

## 13. 真实模型迁移结果与后续切换

项目已经完成从 256 维演示向量到 1024 维真实向量的迁移：

| 表 | 当前用途 | 已验证结果 |
| --- | --- | --- |
| `travel_knowledge_embedding` | 正式 Qwen3 向量表 | 22 条唯一片段，1024 维，模型标记 `Qwen3-Embedding-0.6B@1024` |
| `travel_knowledge_embedding_demo256_backup_20261127` | 旧演示向量备份 | 19 条片段，256 维，模型标记 `demo-feature-hash-v3` |
| `travel_knowledge_chunk_chengdu_backup_20261127_180939` | 成都测试正文备份 | 9 条退役测试片段 |
| `travel_knowledge_embedding_chengdu_backup_20261127_180939` | 成都测试向量备份 | 9 条与正文 ID 对应的旧向量 |

256→1024 向量迁移本身不修改 MySQL；2026-11-27 的知识库重整另行备份并删除了 9 条成都测试正文及对应向量。当前 MySQL 和正式向量表均为 22 条 `GLOBAL` 通用知识，应用重复启动同步数量为 0。正式表的普通索引、HNSW `vector_cosine_ops` 索引和主键均已核对。

以后切换 Embedding 模型或维度时：

1. 在 `application.yml` 中直接配置新的 Embedding 服务地址、模型名称、维度和可选密钥，不使用 `${ENV:default}` 占位符。
2. 确认 `TravelEmbeddingService` 只有一个 Profile 对应的实现，并让 `getModelName()` 区分模型版本和维度。
3. 保持 `embedDocument` 与 `embedQuery` 使用同一模型、版本和维度；不要在两处分别调用不同实现。
4. 停止应用，确认目标 PostgreSQL 数据库，重命名旧表、主键约束和索引，保留可回滚数据。
5. 重启应用自动创建新表并重建向量，再核对全部片段数量、维度、模型标记、索引、`Synchronized 0` 和检索样本。

`CREATE TABLE IF NOT EXISTS` 不会把已有 `VECTOR(256)` 改成 `VECTOR(1024)`。应用不会自动 `DROP TABLE` 或 `TRUNCATE TABLE`；维度不一致时会明确终止启动。完整的迁移 SQL、备份核对和可逆回滚步骤见 [开发与扩展指南](02-development-guide.md#切换-embedding-模型与迁移向量表)。

如需运行演示哈希实现，必须使用 `SPRING_PROFILES_ACTIVE=demo-embedding`、独立 256 维演示数据库和 `--travel.rag.vector.min-similarity=0.15`。不要把演示 Profile 指向当前 1024 维正式库。

### AgentScope Java 学习资料

AgentScope Java 的 `SimpleKnowledge`、`OpenAITextEmbedding` 和 `InMemoryStore` 可以作为学习 RAG 概念的资料，但当前项目明确选择应用层 JDBC + PostgreSQL pgvector，不混用这些内存存储方案，也不套用 Python 文档。Java API 以官方 Java 文档为准。

参考：

- [AgentScope Java RAG 总览](https://java.agentscope.io/v2/zh/integration/rag/overview)
- [AgentScope Java 官方应用层 RAG 示例](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/agentscope-examples/documentation/src/main/java/io/agentscope/examples/documentation2/rag/ApplicationLayerRagExample.java)
