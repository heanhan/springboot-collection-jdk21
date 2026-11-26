# RAG 旅游知识检索学习指南

本文对应旅游多智能体项目已经完成的 RAG 链路：MySQL 保存知识正文，PostgreSQL + pgvector 保存向量，应用层完成 Embedding、候选检索、正文回查和来源引用。

当前知识正文通过 **MySQL 8 + Spring Data JPA** 持久化，共 19 个片段；**PostgreSQL 17.9 + pgvector 0.8.2** 保存对应的 19 条向量，通过 `chunk_id` 关联。当前 Embedding 是 256 维特征哈希演示实现，只用于跑通链路，不具备真正的语义理解。

## 1. 什么是 RAG

RAG 是 Retrieval-Augmented Generation 的缩写：

1. **Retrieval（检索）**：根据用户问题，从知识库找到相关片段。
2. **Augmented（增强）**：把检索片段作为额外上下文放入提示词。
3. **Generation（生成）**：模型基于问题和检索资料生成回答。

当前实现采用应用层 RAG：Java 服务先从 pgvector 找到候选，再从 MySQL 取回正文，最后把材料放进普通 `ReActAgent` 的输入，而不是让 Agent 自主调用向量检索工具。项目明确选择 **应用层 JDBC + pgvector**，不使用 AgentScope Java 的内存 Store 作为当前存储，也不混用 Python 文档方案。

## 2. 当前检索架构

### 启动同步

```text
src/main/resources/knowledge/travel/*.md
                │
                ▼
      TravelKnowledgeService
                │
                ├── 分块并按稳定 ID 同步到 MySQL
                │      travel_knowledge_chunk（19 个正文片段）
                │
                ├── 读取“标题 + 正文”，计算 content_hash
                ├── 按 chunk_id + content_hash + model_name 判断是否最新
                │
                ▼
   DemoFeatureHashEmbeddingService（demo-feature-hash-v3，256 维）
                │
                ▼
      TravelVectorRepository 幂等 upsert
                │
                ▼
PostgreSQL travel_knowledge_embedding（19 条向量）
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
| MySQL `travel_knowledge_chunk` | 保存标题、来源、正文和时间戳 | Spring Data JPA |
| PostgreSQL `travel_knowledge_embedding` | 保存向量、`chunk_id`、模型标记、内容哈希和嵌入时间 | 独立 `JdbcTemplate` + pgvector |

两库没有跨库外键，由应用层通过 `chunk_id` 关联；MySQL 正文缺失的候选会被跳过。当前 20 个单元测试全部通过，真实库已验证 19 条唯一 256 维 `demo-feature-hash-v3` 向量、普通索引和 HNSW 索引，重复启动不会重复生成未变化向量。已知美食和预算查询能返回知识，未知问题返回空结果；真实 Chat 模型的美食问答也保留了 `[1]`、`[2]` 引用并返回两条 `knowledgeSources`。

## 3. 知识文件

当前示例知识位于：

`springboot-agent-scope-travel/src/main/resources/knowledge/travel/`

包含：

- `chengdu-itinerary.md`：成都三天两夜行程。
- `chengdu-food.md`：成都美食与饮食提醒。
- `travel-budget.md`：旅行预算估算方法。
- `travel-safety.md`：旅行安全与信息核实提醒。

这些是项目内置的静态学习资料，不是实时天气、票价或政策数据。

Markdown 文件是初始化素材，不是运行时直接读取的数据源。应用启动时把片段同步到 MySQL，再从数据库加载。后续可以直接通过 JPA 管理知识正文；如果仍保留 Markdown 素材，启动同步会以文件内容更新同 ID 的记录。

## 4. 检索接口

### 请求

```http
POST /api/travel/knowledge/search
Content-Type: application/json
```

```json
{
  "query": "成都三天两夜有什么美食推荐",
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
  "query": "成都三天两夜有什么美食推荐",
  "topK": 3,
  "totalMatches": 1,
  "results": [
    {
      "id": "chengdu-food-1",
      "title": "成都美食指南 - 美食类型",
      "source": "chengdu-food.md",
      "score": 87,
      "content": "美食类型\n成都美食以麻辣、鲜香和多样的小吃文化为代表。"
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
    "query": "成都三天两夜有什么美食推荐",
    "topK": 3
  }'
```

## 5. Markdown 如何变成知识片段

`TravelKnowledgeService` 启动时读取配置匹配的知识文件，按以下规则生成片段，并通过 `TravelKnowledgeChunkRepository` 同步到 MySQL：

1. 一级标题作为文档标题。
2. 二级、三级标题作为片段标题。
3. 空行结束当前段落。
4. 每个非空段落生成一个知识片段。
5. 片段 ID 由文件名和序号组成，例如 `chengdu-food-1`。
6. 来源保存为 Markdown 文件名，便于回答时引用。
7. 根据 ID 查询数据库；不存在时插入，标题、来源或正文变化时更新。
8. 从数据库读取全部片段，对“标题 + 换行 + 正文”计算 SHA-256 `content_hash`。
9. 按 `chunk_id + content_hash + model_name` 判断向量是否最新；缺失或变化时生成向量并幂等写入 pgvector。

`TravelKnowledgeChunk` 对应 MySQL `travel_knowledge_chunk` 表，包含 `id`、`title`、`source`、`content`、`created_at` 和 `updated_at`。同步时保留已有记录的创建时间，只更新内容变化的记录及其更新时间。

PostgreSQL `travel_knowledge_embedding` 保存 `chunk_id`、`embedding`、`model_name`、`content_hash` 和 `embedded_at`。当前模型标记为 `demo-feature-hash-v3`，向量列为 `VECTOR(256)`；`chunk_id` 是主键，`ON CONFLICT (chunk_id)` 保证重复启动和重复同步不会产生重复向量记录。

示例：

```markdown
# 成都美食指南

## 美食类型

成都美食以麻辣、鲜香为代表。
```

会生成一个标题为“成都美食指南 - 美食类型”的知识片段。

## 6. 演示 Embedding 与相似度计算

当前唯一实现是 `DemoFeatureHashEmbeddingService`：

| 项目 | 当前值 |
| --- | --- |
| 模型标记 | `demo-feature-hash-v3` |
| 向量维度 | 256 |
| 特征来源 | 中文二元词、英文单词和数字 |
| 哈希算法 | SHA-256 特征哈希 |
| 归一化 | L2 归一化 |

生成过程：

1. 将文本转小写，并去掉“推荐、当地、怎么、如何、什么”等泛化问句短语，避免它们稀释主题词。
2. 提取英文单词和数字，以及连续中文的相邻二字组合，并过滤常见噪声字。
3. 每个词项通过 SHA-256 映射到两个带正负号的向量桶，减少偶然碰撞的影响。
4. 对 256 维向量做 L2 归一化；极端情况下保证不返回零向量。

这是**假 Embedding**，没有经过语义模型训练，不能理解同义词、上下文或跨语言表达。它的用途是验证建表、写入、索引、距离查询和正文回查，不代表真实 RAG 的检索质量。

检索过程：

1. 使用用户当前问题生成查询向量，不拼接路由理由。
2. `TravelVectorRepository` 只查询当前 `model_name`，执行 `embedding <=> CAST(? AS vector)`，按余弦距离升序取前 `candidate-limit` 个候选。
3. 应用层计算 `similarity = 1 - distance`，过滤低于 `min-similarity` 的候选。
4. 按候选 `chunk_id` 从 MySQL `findAllById` 批量取回正文，再按 pgvector 的向量顺序输出。
5. 同一来源最多保留 2 个片段，最后截取 Top-K；距离相同时按 `chunk_id` 升序。
6. `score` 先将相似度限制在 0–1，再计算 `round(similarity × 100)`，是 0–100 的相似度百分比，不是概率。

当前配置的候选上限为 20、最低相似度为 0.15。文档向量与查询向量必须来自同一个 `TravelEmbeddingService` 实现，不能混用不同算法或模型版本。

## 7. 配置项

部署时建议通过环境变量提供连接信息，下面全部使用占位符：

```yaml
travel:
  rag:
    knowledge-pattern: classpath*:knowledge/travel/*.md
    top-k: 3
    vector:
      url: ${PG_URL}
      username: ${PG_USER}
      password: ${PG_PASSWORD}
      min-similarity: 0.15
      candidate-limit: 20

spring:
  datasource:
    url: jdbc:mysql://<mysql-host>:3306/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
```

`application.yml` 还提供了本地示例默认地址；生产或本地联调时应按实际环境覆盖。启动环境变量示例见 [README](../README.md#6-启动)。

| 配置 | 说明 |
| --- | --- |
| `travel.rag.knowledge-pattern` | 启动同步所需的 Spring Resource 匹配规则 |
| `travel.rag.top-k` | 默认返回片段数量，范围 1 到 10 |
| `travel.rag.vector.url` | PostgreSQL JDBC 地址，通过 `PG_URL` 覆盖 |
| `travel.rag.vector.username` | PostgreSQL 用户名，通过 `PG_USER` 覆盖 |
| `travel.rag.vector.password` | PostgreSQL 密码，`PG_PASSWORD` 启动必填 |
| `travel.rag.vector.min-similarity` | 最低余弦相似度，范围 0 到 1，当前为 `0.15` |
| `travel.rag.vector.candidate-limit` | pgvector 候选上限，当前为 `20` |
| `MYSQL_DATABASE` | 业务库名称 |
| `MYSQL_USER` | MySQL 用户名 |
| `MYSQL_PASSWORD` | 启动时必须提供的 MySQL 密码环境变量 |
| `spring.jpa.hibernate.ddl-auto` | 当前为 `update`，只建议本地学习环境使用 |

调整建议：

- 命中过多：提高 `min-similarity`、降低 `top-k`，或检查知识段落是否过于宽泛。
- 命中过少：先核对两库数量和模型标记，再适当降低 `min-similarity`。
- 结果不相关：优先检查知识正文和 Embedding 实现；当前特征哈希不具备语义能力，替换真实模型比单纯调阈值更有效。
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

SELECT id, title, source, CHAR_LENGTH(content) AS content_length
FROM travel_knowledge_chunk
ORDER BY id;
```

当前基线应为 19 个片段、19 个唯一 ID。记下按 ID 排序的列表，用于和 pgvector 的 `chunk_id` 对比。

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
           WHERE vector_dims(embedding) <> 256
       ) AS wrong_dimension_count
FROM travel_knowledge_embedding;

SELECT model_name, COUNT(*) AS vector_count
FROM travel_knowledge_embedding
GROUP BY model_name
ORDER BY model_name;

SELECT chunk_id
FROM travel_knowledge_embedding
WHERE model_name = 'demo-feature-hash-v3'
ORDER BY chunk_id;

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
  AND tablename = 'travel_knowledge_embedding'
ORDER BY indexname;
```

当前基线应为 19 条唯一 `chunk_id`，最小和最大维度均为 256，错误维度数量为 0，模型标记为 `demo-feature-hash-v3`。`chunk_id` 排序结果应与 MySQL 的 `id` 列表一致。索引结果应包含：

- `idx_travel_knowledge_embedding_model`：`model_name` 普通索引。
- `idx_travel_knowledge_embedding_hnsw`：HNSW 索引，方法为 `vector_cosine_ops`。

可以用一条已存储的向量核对余弦距离和排序，不需要手工拼写 256 个浮点数：

```sql
WITH query_vector AS (
    SELECT embedding
    FROM travel_knowledge_embedding
    WHERE chunk_id = 'chengdu-food-1'
      AND model_name = 'demo-feature-hash-v3'
)
SELECT e.chunk_id,
       1 - (e.embedding <=> q.embedding) AS similarity
FROM travel_knowledge_embedding AS e
CROSS JOIN query_vector AS q
WHERE e.model_name = 'demo-feature-hash-v3'
ORDER BY e.embedding <=> q.embedding, e.chunk_id
LIMIT 20;
```

`chengdu-food-1` 与自身比较时相似度应为 1。这条 SQL 只用于核对数据库距离运算，实际 HTTP 检索仍会先通过当前 Embedding 实现生成查询向量。

## 10. 如何调试

### 没有命中

- 查询 MySQL `travel_knowledge_chunk`，确认对应片段已经同步。
- 查询 PostgreSQL，确认当前 `model_name` 下有对应 `chunk_id`，且向量维度正确。
- 检查两张表的 ID 是否一致；MySQL 正文缺失的候选会被跳过。
- 检查 `min-similarity` 和 `candidate-limit`，必要时调整后重新调用独立检索接口。
- 查看启动日志中的 MySQL 片段数量和向量同步数量。
- 如果问题是语义相近但用词不同的表达，应替换真实 Embedding，不要期待特征哈希理解同义词。

### 命中了错误知识

- 检查标题是否过于宽泛，段落是否混合多个主题。
- 拆分知识段落，使每个片段只表达一个完整知识点。
- 确认文档向量和查询向量使用同一模型、同一版本和同一维度。
- 当前演示 Embedding 只比较特征词重合，语义相近问题较多时应尽快替换真实模型。

### 分数很高但内容不相关

特征哈希可能产生词项碰撞，也无法理解完整语义。相似度百分比只表示当前向量空间中的距离，不是内容正确率。先核对数据库和模型标记，再检查知识内容；不要仅凭高分认定答案可靠。

### 答案没有正文引用

先查看响应的 `knowledgeSources` 是否为空；为空说明没有知识命中。来源非空但正文没有 `[1]` 等标记时，检查专家和指南提示词中的引用要求。来源章节由程序追加，因此即使模型漏写正文引用，也不会丢失来源列表。

## 11. 当前实现边界

- 检索发生在应用层，使用 JDBC + pgvector，不是 Agent 自主调用工具。
- 知识正文持久化在 MySQL，向量持久化在 PostgreSQL，两库通过 `chunk_id` 关联。
- 当前 256 维特征哈希只用于跑通链路，不具备真正的语义理解和语义排序能力。
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
[1] 成都美食指南 - 代表性美食
来源文件：chengdu-food.md
片段编号：chengdu-food-1
可以了解火锅、串串和小吃。

[2] 旅行安全 - 饮食卫生
来源文件：travel-safety.md
片段编号：travel-safety-1
注意饮食卫生，出现不适及时就医。
```

专家 Agent 收到要求：优先依据知识回答，并在相关事实后保留 `[1]`、`[2]` 编号；没有命中时明确说明，不能把模型常识说成知识库内容。指南 Agent 同时收到专家材料和检索知识，并被要求保留这些编号。

### 最终响应如何返回来源

`/api/travel/chat` 的响应新增 `knowledgeSources`：

```json
{
  "sessionId": "chengdu-001",
  "routedAgent": "FOOD",
  "routeReason": "用户想了解成都三天两夜的美食推荐。",
  "answer": "成都美食可以了解火锅和小吃。[1]\n\n### 知识来源\n- [1] `chengdu-food.md` — 成都美食指南 - 代表性美食（片段 ID：`chengdu-food-1`）",
  "notice": "本指南由本地模型生成，未实时查询交通、票价、天气或营业信息，出行前请通过官方渠道核实。",
  "knowledgeSources": [
    {
      "id": "chengdu-food-1",
      "title": "成都美食指南 - 代表性美食",
      "source": "chengdu-food.md",
      "score": 87,
      "content": "可以了解火锅、串串和小吃。"
    }
  ]
}
```

知识来源章节由程序统一追加，不依赖模型自觉生成；`knowledgeSources` 的顺序与 `[1]`、`[2]` 编号一致，其中的 `score` 同为 0–100 相似度百分比。没有命中时返回空数组，且答案不包含知识来源章节。指南 Agent 返回空内容时，服务仍会使用专家回答并保留来源。

## 13. 替换为真实 Embedding 模型

pgvector 链路已经完成，替换时不需要改 MySQL 正文存储、`TravelVectorRepository` 或 HTTP 接口，核心是替换唯一的 `TravelEmbeddingService` Bean：

1. **新增实现**：实现 `getDimension()`、`getModelName()` 和 `embed(String text)`。文本转向量必须返回固定维度，知识同步与查询都注入这一个接口，不要分别调用不同模型。
2. **保证唯一 Bean**：移除 `DemoFeatureHashEmbeddingService` 的 `@Service`，或给 Demo 与真实实现配置互斥 `@Profile`。每个环境只能有一个 `TravelEmbeddingService` Bean。
3. **标记算法和模型版本**：新的 `getModelName()` 必须能区分算法和模型版本，例如包含真实模型名称及版本。不要沿用演示标记，也不要在模型升级后继续使用无法区分的旧标记。
4. **统一文档与查询**：启动同步用“标题 + 换行 + 正文”生成文档向量，查询用用户当前问题生成查询向量；两者必须使用同一实现、版本和维度。同步修改文档、核对 SQL 和查询中的模型标记。
5. **备份并处理旧表**：停止应用，确认目标 PostgreSQL 数据库，并备份 `travel_knowledge_embedding`。不能让应用自动删表。
6. **重启重建并验证**：重新启动应用，按新模型生成全部向量，再检查测试结果、模型标记、维度、唯一片段数、索引和检索质量。

### 维度变化

`CREATE TABLE IF NOT EXISTS` 不会把已有的 `VECTOR(256)` 改成新维度，应用启动时的维度校验也会明确拒绝继续运行。确认目标库并完成备份后，由数据库管理员手工删除旧表，再启动应用自动建表：

```sql
-- 仅在确认目标数据库并完成备份后手工执行。
DROP TABLE travel_knowledge_embedding;
```

应用会按新实现返回的维度重新创建表和索引，再从 MySQL 同步正文和向量。MySQL `travel_knowledge_chunk` 不需要删除。

### 同维度换模型

即使新旧模型维度相同，也建议先备份，再手工清表并重启，让应用重建全部向量，避免旧模型记录残留：

```sql
-- 仅在确认目标数据库并完成备份后手工执行。
TRUNCATE TABLE travel_knowledge_embedding;
```

应用不会自动执行上述 `DROP TABLE` 或 `TRUNCATE TABLE`。清表后应确认新模型记录已经完整生成，再开放检索。

### AgentScope Java 学习资料

AgentScope Java 的 `SimpleKnowledge`、`OpenAITextEmbedding` 和 `InMemoryStore` 可以作为学习 RAG 概念的资料，但当前项目明确选择应用层 JDBC + PostgreSQL pgvector，不混用这些内存存储方案，也不套用 Python 文档。Java API 以官方 Java 文档为准。

参考：

- [AgentScope Java RAG 总览](https://java.agentscope.io/v2/zh/integration/rag/overview)
- [AgentScope Java 官方应用层 RAG 示例](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/agentscope-examples/documentation/src/main/java/io/agentscope/examples/documentation2/rag/ApplicationLayerRagExample.java)
