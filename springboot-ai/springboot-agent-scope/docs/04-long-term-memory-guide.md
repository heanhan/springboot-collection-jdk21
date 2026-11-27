# 长期记忆学习指南

本文对应旅游 Demo 已经接入的 AgentScope Java 2.0.4 长期记忆链路，说明两层记忆、用户隔离、异步写入、提示词边界和验证方法。

> `docs.agentscope.io/zh/versions/2.0.12` 是 Python 版文档，不能直接套用到 Java。Java 核心模块旧的长期记忆 API 已弃用并计划移除，当前项目使用官方 `agentscope-harness:2.0.4` 提供的 `HarnessAgent`。

## 1. 记忆解决什么问题

Agent 的会话状态和长期记忆是两套机制：

| 机制 | 保存内容 | 主要用途 |
| --- | --- | --- |
| `JsonFileAgentStateStore` | 当前会话的消息和 Agent 状态 | 同一 `sessionId` 内继续多轮对话 |
| 每日记忆日志 | 从对话中抽取的用户事实 | 保留原始抽取结果，便于整理和排查 |
| `MEMORY.md` | 去重后的长期记忆 | 同一用户跨会话提供偏好、约束和历史决策 |

例如，用户第一次说“以后旅行节奏舒缓一些”，第二次使用新的 `sessionId` 询问行程时，Agent 仍应知道该偏好。这依赖长期记忆，而不是会话状态。

## 2. 当前架构

```text
TravelGuideService
  │
  ├── travel-router / specialists
  │     关闭自动记忆 hooks
  │     读取 MEMORY.md，使用记忆召回工具
  │
  └── travel-guide
        开启自动记忆 hooks
        │
        ├── 调用结束后异步抽取
        │      MemoryFlushManager
        │      → memory/YYYY-MM-DD.md
        │
        └── 满足整理条件后异步合并
               MemoryConsolidator
               → MEMORY.md
               → memory/.consolidation_state

TravelGuideService
  └── 补偿整理：等待 Harness 后台任务静默后，按水印再执行一次整理
```

职责边界：

- Router 和专家只读使用记忆，不负责写入。
- Guide 是唯一开启自动记忆 hooks 的 Agent。
- 所有 Agent 都移除了 `memory_save`，长期记忆统一由自动 hooks 写入，避免模型主动保存和自动抽取重复记录。
- 所有 Agent 都移除了 `wait_async_results`，旅游场景没有子 Agent 异步任务。
- Toolkit 仅保留 `memory_search`、`memory_get`、`session_search`、`session_list`、`session_history`。

## 3. 两层记忆文件

默认工作区由以下配置指定：

```yaml
travel:
  agent:
    memory-workspace-directory: .agentscope-data/travel-memory
```

目录结构：

```text
.agentscope-data/travel-memory/
└── <userId>/
    ├── MEMORY.md
    └── memory/
        ├── 2026-11-26.md
        └── .consolidation_state
```

### 每日日志

每次 Guide 调用结束后，`MemoryFlushManager` 将新记忆追加到当天的 `memory/YYYY-MM-DD.md`。日志按时间分段，当前只应出现 `Memory Flush` 段落。

每日日志是追加写的，适合保留抽取来源和发生时间，但可能包含跨多次抽取的重复表达，因此不能直接当作最终长期记忆。

### curated 长期记忆

`MemoryConsolidator` 定期读取：

1. 当前 `MEMORY.md`。
2. 最近一次整理水印之后发生变化的每日日志。

然后调用 LLM 合并、去重、删除过时信息，并覆盖写入完整的 `MEMORY.md`。整理成功后更新 `memory/.consolidation_state`。

`MEMORY.md` 是跨会话推理时使用的主要长期记忆，必须保持简洁、独立和可复用。

## 4. 用户隔离

`TravelAgentConfiguration` 使用：

```java
new LocalFilesystemSpec()
    .project(memoryWorkspace)
    .mode(LocalFsMode.SANDBOXED)
    .isolationScope(IsolationScope.USER);
```

效果：

- 同一 `userId` 的不同 `sessionId` 共享 `MEMORY.md` 和每日日志。
- 不同 `userId` 使用不同目录，不共享记忆文件。
- `userId` 为空时 Harness 会回退到 `sessionId`，因此正式环境必须始终提供真实用户 ID。
- 项目目录显式指向记忆工作区，避免本地文件系统回退读取宿主应用源码。

当前 HTTP 服务固定使用 `local-demo-user`，这是本地 Demo 的简化。正式接入认证后，应从认证上下文取得用户 ID，不能相信请求体随意传入的 `userId`，否则会造成越权读取或写入。

## 5. 异步行为与频率

记忆 hooks 不会阻塞当前 HTTP 请求：

1. Guide 返回答案后，请求即可结束。
2. 后台抽取模型继续运行并写入每日日志。
3. 后台维护任务按条件整理 `MEMORY.md`。
4. `TravelGuideService` 启动补偿任务，等待当前 Harness 后台记忆任务静默，再按整理水印执行一次 `MemoryConsolidator`；如果官方维护已经整理成功，补偿任务会因为没有新日志而空转。

Harness 2.0.4 的抽取和首次维护可能并发执行，维护任务有可能先读到空日志并跳过整理。项目增加补偿任务，避免首次抽取完成后仍长时间没有 `MEMORY.md`，导致新会话无法稳定召回。

因此，请求返回后立即读取文件可能看不到新记忆。验证时需要等待或轮询文件，也可以先关闭应用，让 Harness 执行有限的后台任务收尾，但仍应检查最终文件。

当前 `MemoryConfig` 策略：

| 配置 | 当前值 |
| --- | --- |
| `flushTrigger` | `always()`，每次 Guide 调用后允许抽取 |
| `consolidationMinGap` | 30 分钟，后续整理之间的最小间隔 |
| `consolidationMaxTokens` | 4000 |
| `dailyFileRetentionDays` | 90 天 |
| `sessionRetentionDays` | 180 天 |

首次整理在首次符合条件时执行，30 分钟不是首次延迟。补偿任务只在后台任务静默后按水印检查是否有新日志，不会无条件重写 `MEMORY.md`。抽取和整理都会调用模型，可能产生额外 Token 开销；需要降低成本时，可以配置专用轻量模型或调整触发频率。

## 6. 旅游提示词规则

提示词集中在 `TravelAgentPrompts`。

### 抽取规则

`MEMORY_FLUSH_PROMPT` 只保留用户明确表达且未来可能复用的信息：

- 稳定偏好：旅行节奏、交通方式、住宿类型、饮食禁忌、预算区间等。
- 明确约束：目的地、天数、人数、出行时间、无障碍服务级别等。
- 历史决策及用户明确说明的原因。
- 未来服务该用户所必需的个人事实。

只能从标记为 `[USER]` 的用户消息中提取事实。`[ASSISTANT]` 和 `[TOOL]` 内容只能帮助理解，不能作为记忆来源；助手补充的通用建议、推测和攻略不得记录，即使用户没有反对。用户的问题、请求或查询语句本身也不是长期记忆，例如“我的旅行节奏是什么”应输出 `NO_REPLY`，只有“我偏好舒缓节奏”这类明确陈述才需要记录。

以下内容不进入长期记忆：

- 临时寒暄、一次性查询和短期状态。
- 模型假设、RAG 公共知识、专家生成的攻略。
- 实时价格、天气、班次、营业信息和短期政策。
- 密码、API 密钥、证件号码、支付信息、完整联系方式、精确住址和详细病史等敏感信息。

没有值得记录的新信息时，抽取模型输出 `NO_REPLY`。

### 整理规则

`MEMORY_CONSOLIDATION_PROMPT` 将每日日志合并为完整、简洁的 `MEMORY.md`：

- 按“用户偏好、旅行约束、历史决策、用户事实”分组。
- 同一事实只保留一条。
- 新的明确更正优先于旧信息。
- 无法确认时间或发生冲突时不猜测。
- 不编造用户没有提供的日期、原因或事实。

自定义整理提示词必须恰好包含两个 `%d` 占位符，顺序为最大 Token 数和近似字符数。`MemoryConfig.Builder` 会在构建时校验。

### 使用规则

所有 Agent 都遵循：

- 当前用户消息中的明确信息优先于历史记忆。
- 历史记忆只能补充未冲突的偏好、约束和历史决策。
- 记忆缺失时按未知处理，不猜测用户偏好。
- 不主动复述敏感信息。
- 描述用户偏好、约束、历史决策或个人事实的句子禁止添加 `[1]`、`[2]` 等知识引用；混合内容必须拆句，并在输出前逐句检查。

## 7. 与 RAG 的边界

| 内容 | 来源 | 是否可添加知识引用 |
| --- | --- | --- |
| 目的地、美食、预算和安全知识 | MySQL 正文 + pgvector 检索 | 可以，使用 `[1]`、`[2]` 等编号 |
| 用户偏好、旅行约束、历史决策 | 用户长期记忆 | 不可以 |
| 用户本轮明确给出的条件 | 当前消息 | 不可以 |
| 模型假设和专家攻略 | 生成内容 | 不得伪装成用户事实或检索知识 |

RAG 是项目公共旅游知识，长期记忆是用户个人上下文。`TravelGuideService` 会把 `MEMORY.md` 以“个人上下文，禁止添加知识引用”的独立段落同时提供给专家和指南 Agent，减少小模型混淆两类来源。提示词还要求模型不得把记忆内容当作 RAG 结果，也不得为记忆内容添加检索引用编号。

## 8. 本地验证

### 写入偏好

使用一个会话保存偏好：

```bash
curl -X POST http://localhost:8080/api/travel/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "memory-write-001",
    "message": "以后帮我规划旅行时，请默认安排节奏舒缓、每天不超过两个主要景点，住宿优先选择地铁站附近。请记住这些偏好。"
  }'
```

等待后台任务完成后检查：

```bash
find .agentscope-data/travel-memory/local-demo-user -maxdepth 3 -type f -print
```

预期看到：

```text
local-demo-user/MEMORY.md
local-demo-user/memory/YYYY-MM-DD.md
local-demo-user/memory/.consolidation_state
```

每日日志应只包含 `Memory Flush` 段落，不应出现 `Memory Save` 重复段落。

### 跨会话召回

使用新的 `sessionId`，不要重复偏好：

```bash
curl -X POST http://localhost:8080/api/travel/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "sessionId": "memory-read-002",
    "message": "请根据我的旅行偏好，推荐一个适合三天两夜的目的地，并说明安排思路。"
  }'
```

预期回答体现“节奏舒缓、每天不超过两个主要景点、优先选择地铁站附近住宿”。

### 用户隔离

HTTP Demo 固定用户，不能通过请求体切换用户。自动化测试 `TravelAgentConfigurationTest` 已验证：

- 同一用户跨会话共享 `MEMORY.md`。
- 不同用户读取不到对方记忆。
- 运行时路径按 `<userId>` 分开。

需要真实模型验证不同用户时，应直接创建 `HarnessAgent`，分别传入不同的 `RuntimeContext.userId`，不要临时给 HTTP 接口增加不可信的身份参数。

## 9. 常见问题

### 请求成功后没有记忆文件

先确认调用的是 Guide 链路，且没有关闭自动记忆 hooks。然后检查模型抽取是否返回 `NO_REPLY`、后台任务是否仍在运行，以及 `memory-workspace-directory` 是否指向预期目录。

### 首次请求后只有每日日志，没有 `MEMORY.md`

当前版本会在 Guide 返回后启动补偿整理，等待 Harness 后台任务静默，再按水印执行一次 `MemoryConsolidator`。如果仍然缺失，检查应用日志中的 `Memory repair consolidation failed` 或 `Timed out waiting for memory background tasks`，并确认本地模型在补偿期间可用。不要通过删除 `.consolidation_state` 来绕过问题，应先确认每日日志和整理日志。

### 新会话没有召回偏好

依次检查：

1. 每日日志是否已经生成。
2. `MEMORY.md` 是否已经整理完成。
3. 两次请求是否使用同一 `userId`。
4. 新会话是否错误地读取了工作区根目录的共享文件。
5. 用户本轮是否明确更正了旧偏好。

### 日志出现重复段落

当前版本已移除 `memory_save`。如果仍出现 `Memory Save`，说明运行的是旧包或存在其他主动写入路径，应重新打包并检查 Toolkit 工具列表。

### 记住了助手生成的建议

检查抽取提示词是否仍要求只从 `[USER]` 消息提取。若旧日志已经包含错误内容，需要人工修正每日日志和 `MEMORY.md`，不能期待下一次整理自动识别所有错误。

### 不同用户看到相同记忆

检查 `RuntimeContext.userId` 是否为空、是否错误回退到 `sessionId`，以及工作区根目录是否存放了共享 `MEMORY.md`。正式环境还要检查认证上下文，不能接受客户端随意传入用户 ID。

## 10. 当前验证结果

- 25 项单元测试全部通过。
- 真实本地模型已完成同一用户不同 `sessionId` 的偏好写入和召回。
- 每日日志、`MEMORY.md` 和 `.consolidation_state` 均已实际生成。
- 移除 `memory_save` 后，每日日志只保留自动 `Memory Flush` 段落，没有重复保存。
- 首次抽取与维护并发时，补偿整理能够生成 `MEMORY.md` 和整理水印。
- 真实本地模型直连已验证不同 `userId` 隔离：Alice 的记忆只写入 Alice 目录，Bob 的回答明确表示没有相关记录。
- 真实模型跨会话回答能够召回偏好，且记忆句子不再添加 RAG 引用编号。
- 自动化测试已验证不同 `userId` 的文件目录隔离。

长期记忆仍依赖本地文件系统和 LLM 抽取质量，不适合直接作为生产级多租户存储。生产环境需要接入认证、共享或分布式存储、备份、权限控制和审计。

## 11. 参考资料

- [AgentScope Java 2.0 官方文档](https://java.agentscope.io/v2)
- [Harness Memory](https://java.agentscope.io/v2/en/docs/harness/memory.md)
- [Harness Workspace](https://java.agentscope.io/v2/en/docs/harness/workspace.md)
- [Harness Architecture](https://java.agentscope.io/v2/en/docs/harness/architecture.md)
- [Middleware](https://java.agentscope.io/v2/en/docs/building-blocks/middleware.md)
- [Runtime Context](https://java.agentscope.io/v2/en/docs/building-blocks/context.md)
