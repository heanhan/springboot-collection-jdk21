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

先准备 MySQL 8 业务库、PostgreSQL 17.9 和目标数据库，并确认 PostgreSQL 服务器已安装 pgvector 0.8.2。以下环境变量均为占位符：

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

MySQL 主机地址通过 `spring.datasource.url` 配置或启动参数覆盖；数据库名和用户名分别由 `MYSQL_DATABASE`、`MYSQL_USER` 覆盖。`MYSQL_PASSWORD` 和 `PG_PASSWORD` 都是启动必填项，不要把真实密码写入配置文件或提交到仓库。

应用首次启动时，`TravelVectorRepository` 会自动执行 `CREATE EXTENSION IF NOT EXISTS vector`、创建 `travel_knowledge_embedding` 表、`model_name` 普通索引和 HNSW `vector_cosine_ops` 索引。PostgreSQL 账号需要能连接目标数据库、创建扩展、建表、建索引并读写向量表；扩展文件需要预先安装在服务器上，具体权限取决于安装方式。

随后应用把 Markdown 片段同步到 MySQL，按照 `content_hash + model_name` 判断向量是否最新，并将缺失或变化的向量幂等写入 pgvector。当前种子数据为 19 个片段、19 条 256 维向量。重复启动不会重复插入相同片段 ID，也不会重复生成未变化的向量。已有向量表不会自动改变维度；启动时若发现现有向量维度与当前 Embedding 不一致，会明确终止启动，避免误写，应用不会自动删表。

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
- 仅保留记忆读取与会话召回工具，不暴露主动保存、文件、Shell、Web、子 Agent 和 Skills 能力。

### 第四层：业务编排

阅读 `TravelGuideService#chat`。

这是项目核心，依次完成：

1. 调用路由 Agent。
2. 解析 `RouteDecision`。
3. 只使用用户当前问题检索知识，避免路由理由污染查询。
4. `TravelKnowledgeService` 生成查询向量，从 pgvector 取候选，经阈值过滤后从 MySQL 批量取回正文。
5. 按向量顺序、每来源最多 2 个和 Top-K 规则组装带编号的知识材料。
6. 把知识材料交给对应专家 Agent，再把专家材料和检索知识交给指南 Agent。
7. 统一追加知识来源并组装 `TravelChatResponse`。

### 第五层：RAG 数据链路

阅读 `TravelEmbeddingService`、`DemoFeatureHashEmbeddingService`、`TravelVectorRepository` 和 `TravelVectorConfiguration`。

需要关注：

- MySQL 保存正文，PostgreSQL 保存向量，两库通过 `chunk_id` 关联。
- 当前唯一 Embedding Bean 的模型标记为 `demo-feature-hash-v3`，维度为 256。
- 启动时按 `content_hash + model_name` 判断是否最新，通过 `ON CONFLICT (chunk_id)` 幂等写入。
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
| `travel.rag.knowledge-pattern` | Markdown 初始化素材的匹配规则 |
| `travel.rag.top-k` | 默认返回片段数量，范围 1 到 10 |
| `travel.rag.vector.url` | PostgreSQL JDBC 地址，可通过 `PG_URL` 覆盖 |
| `travel.rag.vector.username` | PostgreSQL 用户名，可通过 `PG_USER` 覆盖 |
| `travel.rag.vector.password` | 从必填的 `PG_PASSWORD` 环境变量读取 |
| `travel.rag.vector.min-similarity` | 最低余弦相似度，范围 0 到 1，当前为 `0.15` |
| `travel.rag.vector.candidate-limit` | pgvector 候选上限，当前为 `20` |
| `spring.datasource.url` | MySQL 业务库地址 |
| `spring.datasource.username` | MySQL 用户名，可通过 `MYSQL_USER` 覆盖 |
| `spring.datasource.password` | 从必填的 `MYSQL_PASSWORD` 环境变量读取 |
| `spring.jpa.hibernate.ddl-auto` | 当前为 `update`，仅建议本地学习使用 |

`TravelAgentProperties`、`TravelRagProperties` 和 `TravelVectorProperties` 中的字段默认值与 YAML 配置共同决定最终参数。

### 替换为真实 Embedding 模型

当前 `DemoFeatureHashEmbeddingService` 的模型标记为 `demo-feature-hash-v3`，只用于跑通链路：它先转小写并过滤“推荐、当地、怎么、如何”等泛化短语，再基于 SHA-256 特征哈希、中文二元词和英文词生成 256 维 L2 归一化向量，不理解同义词和完整语义，不能把演示分数当作真实模型效果。替换时按以下步骤操作：

1. **新增实现**：新增一个 `TravelEmbeddingService` 实现，返回真实模型的固定维度、模型标记和文本向量。知识同步与查询都继续注入这一个接口，不要在两处分别调用不同模型。
2. **保证唯一 Bean**：移除 Demo 实现的 `@Service`，或给 Demo 与真实实现配置互斥 `@Profile`。任何运行环境都必须恰好只有一个 `TravelEmbeddingService` Bean，否则 Spring 注入会失败。
3. **区分版本**：`getModelName()` 必须能区分算法和模型版本，例如包含 Embedding 模型名称及版本；不要沿用 `demo-feature-hash-v3`，也不要只写一个可能升级的宽泛名称。
4. **统一文档与查询**：文档向量使用“标题 + 换行 + 正文”，查询向量使用用户当前问题，二者必须来自同一实现、同一版本和同一维度。同步修改本文档、查询 SQL 和核对命令中的模型标记。
5. **处理旧向量**：停止应用并确认目标数据库。先备份 `travel_knowledge_embedding`，再按维度决定处理方式，不能让应用自动删表。
6. **重建并验证**：重新启动应用，让它按新模型重建向量；随后执行单元测试，并核对模型标记、维度、唯一 `chunk_id` 数量、索引和独立检索结果。

维度变化时，`CREATE TABLE IF NOT EXISTS` 不会修改已有的 `VECTOR(256)`。确认并备份后，手工删除旧表，再启动应用按新维度自动建表：

```sql
-- 仅在确认目标数据库并完成备份后，由数据库管理员手工执行。
DROP TABLE travel_knowledge_embedding;
```

即使维度不变，也建议换模型后先备份，再手工清表并重新启动，让应用重建全部向量，避免旧模型记录残留：

```sql
-- 仅在确认目标数据库并完成备份后，由数据库管理员手工执行。
TRUNCATE TABLE travel_knowledge_embedding;
```

应用只创建不存在的表和索引，不提供自动删表或自动迁移向量维度的功能。启动时若检测到现有向量维度与当前实现不一致，会明确失败并提示人工备份、重建，不会继续写入。MySQL 的 `travel_knowledge_chunk` 正文不受影响，不需要随之删除。

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
- `base-url` 是否可访问。
- `model-name` 是否与模型列表一致。
- `api-key` 是否满足本地服务要求。
- 配置中的温度和 Token 数是否在校验范围内。
- MySQL 是否已创建目标数据库，`MYSQL_PASSWORD` 是否已设置且账号允许连接。
- `PG_URL`、`PG_USER` 是否指向正确数据库，必填的 `PG_PASSWORD` 是否已设置。
- PostgreSQL 服务器是否已安装 pgvector 0.8.2，账号是否有创建扩展、表和索引的权限。
- MySQL `travel_knowledge_chunk` 是否成功创建并加载 19 个片段。
- PostgreSQL `travel_knowledge_embedding` 是否存在，向量维度和索引是否符合当前 Embedding 实现。

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

先调用 `/api/travel/knowledge/search` 确认是否命中，再检查 `travel.rag.vector.min-similarity`、`travel.rag.vector.candidate-limit` 和 `travel.rag.top-k`。同时核对 MySQL 片段数量、PostgreSQL 当前模型的向量数量与维度，具体 SQL 见 [RAG 学习指南](03-rag-learning-guide.md#9-核对-mysql-与-pgvector-数据)。当前特征哈希 Embedding 不理解真正语义，同义表达命中差时应先替换模型，不要仅凭调低阈值判断链路正常。问答接口不会返回独立检索的原始过程，但响应的 `knowledgeSources` 可以说明本次检索到并交给 Agent 的片段。

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

- [ ] Maven 干净打包成功，25 个单元测试全部通过。
- [ ] 本地 Chat 模型列表可以访问。
- [ ] MySQL `travel_knowledge_chunk` 中有 19 个知识片段。
- [ ] PostgreSQL `travel_knowledge_embedding` 中有 19 条唯一 `chunk_id`，当前模型均为 256 维，普通索引和 HNSW 索引存在。
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
