# 中医多智能体辅助辨证系统技术需求文档

- 文档版本：V1.3
- 文档状态：开发设计基线，尚未实施；不代表已经完成接口联调或临床验证。
- 本版补充：固定 Spring AI Alibaba 1.1.2.2，明确框架与应用职责、Skills 分层读取、步骤工具撤权、最终请求守卫、专家历史隔离、冻结前会诊准备、系统引用状态复核及最小验证链路；保留既有分流、独立四诊 SOP、分层记忆与上下文压缩设计。
- 适用范围：一期本地开发、合成病例验证及后续真实 HIS 接入预留。
- 需求优先级：用户最新确认的需求高于此前讨论稿；RAG 服务不配置 MySQL，Mem0 使用独立 PostgreSQL＋pgvector 记忆库，不与 RAG 混表或跨库读表。
- 阅读对象：后端开发、AI/RAG 开发、测试、集成与医学审核人员。

## 1. 目标与范围

### 1.1 业务目标
HIS 已完成门诊或住院病历录入。本系统作为 HIS 下游，基于当前患者资料组织多个中医 AI Agent 辅助辨证；医生可以继续讨论、补充事实、指定专家会诊，并审核结果。

“分诊”指选择参与分析的 Agent，不自动改变 HIS 科室、挂号或诊疗安排。“开放问诊”指医生与 Agent 的多轮交互，不要求患者重新完成一套问诊。

交互采用“客服式接诊分流助理→具有相应症状诊疗专长的中医专家 Agent→医生审核”的模式。助理依据患者主诉、已记录症状及必要背景分配专家；专家读取自己的 SKILL.md，按其学派和专长的 SOP 核对望、闻、问、切资料并开展辅助辨证，各步骤按需调用 MCP 工具。这里的“四诊治疗流程”指对临床诊疗流程的辅助，不新增患者自助诊疗、自动处方或医嘱执行权限。

### 1.2 已确认需求
| 编号 | 需求 |
|---|---|
| FR-01 | 以 patientId、visitId、pageSource 获取当前患者资料；HIS 接口暂用模拟服务 |
| FR-02 | 客服式接诊分流助理根据主诉、症状及适用范围匹配具有对应专长的中医 Agent，支持主诊和按需会诊 |
| FR-03 | 每位中医 Agent 拥有独立 SKILL.md，描述自身四诊与辅助辨证 SOP；每个步骤可声明并按需通过 tool calling 执行 MCP 工具 |
| FR-04 | 医生可追问、补充事实及审核辅助辨证报告；不自动开方或执行医嘱 |
| FR-05 | 当前业务系统内部数据使用 MySQL＋Spring Data JPA |
| FR-06 | 独立 RAG 微服务使用 PostgreSQL＋pgvector，提供文献和相似病例能力；不配置 MySQL |
| FR-07 | RAG 支持文献文件上传、结构化病例导入，以及医生主动提交后审核收录 |
| FR-08 | 本地模型由 oMLX 部署，通过 OpenAI 兼容接口调用 |
| FR-09 | 会话历史持久化、各 Agent 短期上下文隔离，支持长对话压缩和跨实例恢复已提交会话 |
| FR-10 | 长期记忆以 MySQL 为权威记录；一期用合成病例验证后正式集成 Mem0，提供受控跨会话检索及关闭降级 |
| FR-11 | 每次模型调用前按实际 token 预算检查；渐进式披露按需加载、去重、压缩和回取，不静默丢弃关键临床信息 |
| FR-12 | 分流可说明症状匹配依据；SOP 步骤、工具结果、缺项及终止原因可追溯，缺少合适专家或必要资料时不强行诊疗 |

### 1.3 一期开发默认值
以下是为了可直接拆解开发而确定的工程默认值，不是已经验证的生产参数：
- 后端优先，提供 REST 与 MCP 接口，暂不建设完整前端。
- 一个助理、两名起步专家、一个汇总节点；每轮一个主诊、至多一个会诊，默认串行。
- 文献支持文本型 PDF、DOC、DOCX；扫描件标记待 OCR，一期不实现 OCR。
- 原始文件保存在 RAG 专用本地目录，后续可替换为对象存储。
- 不引入 Redis、消息队列、服务注册中心；用配置地址发现服务，用自有数据库任务表管理异步工作。
- RAG 普通表使用 Spring JDBC，向量使用 Spring AI PgVectorStore/JDBC；RAG 不需要 JPA。
- tcm 的原始消息、摘要、记忆权威记录及同步任务使用 MySQL＋JPA；Mem0 仅作为可重建的语义检索投影。
- Mem0 适配服务使用 Python 3.12＋FastAPI＋Mem0 OSS，单独部署，不作为 Maven 子模块；SDK/镜像在 D2 验证后锁定版本，不使用浮动 latest。
- 上下文初始压缩阈值为输入预算的70%/85%，目标55%；模型窗口没有通用默认值，32K仅用于第12.4节计算示例。
- 开发数据仅使用合成病例及可合法使用的文献。病例合成标记不得在后续发布中丢失。

### 1.4 非一期范围
- 真实患者自助问诊、自动处方、药物剂量、自动医嘱及 HIS 回写。
- 自动随访、自动把问诊记录训练进模型、自动修改 SOUL.md/SKILL.md。
- 真实身份认证平台、生产 HIS 接口实现、跨院数据共享和生产临床上线。
- 自动判断扫描舌象、音频诊断、OCR、互联网自动抓取文献。
- 多模型投票形成确诊、端到端医疗效果保证。

## 2. 架构与工程划分

### 2.1 服务结构
```text
医生 / HIS 下游入口
        │ REST
        ▼
springboot-ai-alibaba-tcm ── OpenAI 兼容 API ── oMLX 聊天模型
        │
        ├── JPA ── MySQL：会话、事实、报告、审核、摘要、记忆权威记录、同步任务
        ├── 内部 REST ── tcm-memory-service（Mem0 OSS 适配）
        │                    ├── 独立 PostgreSQL＋pgvector：记忆投影、索引、接收回执
        │                    └── 本地 OpenAI 兼容 API：Embedding／可选候选抽取
        ├── MCP ── springboot-ai-alibaba-tcm-mock-mcp ── 合成 HIS 数据
        └── MCP ── springboot-ai-alibaba-tcm-rag
                         ├── PostgreSQL 普通表：文献/病例/来源/审核/任务
                         ├── pgvector：文献片段及病例检索向量
                         ├── OpenAI 兼容 Embedding API ── 本地 Embedding 模型
                         └── 专用文件目录：原始 PDF/Word

资料管理员 ── REST ── RAG 上传、审核、撤回与任务管理
医生主动提交病例 ── tcm Outbox ── REST ── RAG 待审核病例
报告展示／提交前校验 ── tcm 内部 REST ── RAG 引用状态复核（不调用模型）
```

### 2.2 Maven 模块
模块均位于 `springboot-ai/springboot-ai-alibaba-items/`：
| 模块 | 状态 | 职责 |
|---|---|---|
| springboot-ai-alibaba-items | 已存在，packaging=pom | 聚合与版本管理，不放业务代码 |
| springboot-ai-alibaba-tcm | 已存在，占位工程 | 医生接口、Agent 编排、MCP Client、MySQL 业务持久化 |
| springboot-ai-alibaba-tcm-mock-mcp | 待新增，jar | 真实 MCP 协议的模拟 HIS Server，不调用模型 |
| springboot-ai-alibaba-tcm-rag | 待新增，jar | RAG MCP Server、资料管理、Embedding、pgvector |

不修改 hello 示例的业务行为。三个 Java 服务可独立启动、部署，不通过 Maven 依赖对方的实现类。另设 `springboot-ai-alibaba-tcm/services/tcm-memory-service/` 作为待实现的 Python 记忆适配服务目录，通过内部 REST 接入；本文件不代表已创建该目录或完成 Mem0 联调。

RAG 库和记忆库在开发环境可共用 PostgreSQL 实例，但数据库、账号、表及生命周期分离。tcm 不直接连接记忆库，RAG 不访问记忆库；Mem0 的内部历史存储、实体索引等附加状态须随锁定版本核查，不能将“已配置 pgvector”解释为全部状态已支持多实例。

### 2.3 技术基线
| 项目 | 基线 |
|---|---|
| JDK / Spring Boot | 21 / 3.5.16，继承根工程 |
| Spring AI Alibaba / Spring AI | 1.1.2.2 / 1.1.2，继承 items |
| Agent | spring-ai-alibaba-agent-framework；外层 StateGraph 确定性编排，内层 ReactAgent 执行受限任务 |
| Chat / Embedding | Spring AI OpenAI 集成；分别配置模型 |
| MCP | Spring AI MCP Client/Server，Streamable HTTP，端点 `/mcp` |
| tcm 数据访问 | Spring Data JPA＋MySQL Connector/J |
| RAG 数据访问 | Spring JDBC＋PostgreSQL 驱动＋pgvector 集成 |
| 数据迁移 | Flyway；tcm 的 JPA 使用 ddl-auto=validate |
| 文件解析 | Apache PDFBox；Apache POI，DOC 需包含对应解析组件 |
| 集成测试 | JUnit 5、Spring Boot Test、Testcontainers MySQL/PostgreSQL＋pgvector |

开发环境默认选 MySQL 8.4、PostgreSQL 16＋pgvector 0.8.x；交付时锁定具体镜像标签或摘要，不使用浮动 latest。

注意根 POM 显式覆盖了 Hibernate 6.2.7.Final。tcm 引入 JPA 时应局部对齐 Spring Boot 3.5.16 管理的 Hibernate 依赖集合并验证，不直接沿用旧覆盖，不修改无关模块。RAG 不引入 Hibernate。

### 2.4 包与依赖约束
- tcm 根包 `com.example.ai.alibaba.tcm`，按 `api/application/domain/infrastructure/agent` 分层。
- RAG 根包 `com.example.ai.alibaba.tcm.rag`，按 `api/mcp/ingestion/retrieval/domain/infrastructure` 分层。
- 模拟 HIS 根包 `com.example.ai.alibaba.tcm.mockhis`。
- tcm 不连接 PostgreSQL，RAG 不连接 MySQL；服务之间不得跨库读表。
- 不同时引入 DashScope 与本地 OpenAI starter，不引入 AgentScope starter。
- Spring AI Alibaba 固定为 1.1.2.2，Spring AI 固定为 1.1.2；不因官网推荐表仍列其他版本而降级。框架、Graph、Skills 的 API 和默认行为以 1.1.2.2 对应源码及测试为准，MCP SDK 以该依赖组合实际解析版本为准。
- 官网教程及其 main 分支示例仅作为接入参考；D2 记录实际依赖树、源码版本、接入点和验证结果。不将基础工程编译通过等同于 Agent/Skills/MCP/oMLX 链路通过。
- 记忆业务放在 `application/memory`、`domain/memory`，上下文编排放在 `agent/context`，Mem0 REST 适配放在 `infrastructure/memory`；框架 Hook 和 SDK API 不泄露到领域层。

## 3. 身份、就诊上下文与数据契约

### 3.1 当前就诊上下文
| 字段 | 类型/约束 | 来源及用途 |
|---|---|---|
| organizationId | string，1～64 | 从服务端认证上下文取得，不信任请求自行声明 |
| operatorId | string，1～64 | 从服务端认证上下文取得 |
| patientId | string，1～64，必填 | HIS 范围内患者标识，不保证跨院唯一 |
| visitId | string，1～64，必填 | 当前就诊或住院标识，须核验属于 patientId |
| pageSource | enum，必填 | 内部 OUTPATIENT / INPATIENT；真实 HIS 值以后适配 |
| analysisAsOf | RFC3339 时间，可选 | 分析截至时间，默认服务端当前时间 |
| chiefComplaint | string，最多 4000 字符，可选 | HIS 入口透传主诉；读取病历前标记为未核验 |
| identityVerification | object，可选 | 有业务必要时用于后端身份核验，不进入模型 |

身份证不作为分析必填字段、会话 ID、模型输入或检索字段。开发期仅使用虚构测试身份。身份证不是访问患者数据的授权凭证。

### 3.2 患者事实快照
每个快照包含：基础临床信息、主诉、现病史、既往史、过敏、用药、四诊、检查检验、来源清单与完整性状态。

每条事实统一表示为：
```json
{
  "factId": "f-001",
  "field": "chiefComplaint",
  "value": "合成病例主诉",
  "status": "RECORDED",
  "sourceType": "HIS_DOCUMENT",
  "sourceId": "doc-001",
  "sourceVersion": "1",
  "recordedAt": "2026-09-21T09:00:00+08:00",
  "locator": "主诉段",
  "confirmedBy": null
}
```
- `status`：RECORDED、EXPLICITLY_DENIED、NOT_RECORDED、CONFLICT。
- `sourceType`：HIS_DOCUMENT、HIS_STRUCTURED、DOCTOR_SUPPLEMENT、ENTRY_UNVERIFIED。
- HIS 查询失败记在完整性状态中，不转换为“患者否认”或正常结果。
- AI 推测单独保存为候选，不属于事实；假设讨论永不自动写入快照。
- HIS 已有舌脉记录可引用，不默认“远程脉象缺失”；没有的内容才标记缺失。

### 3.3 版本与时间
- 事实快照不可变；医生补充、更正或刷新 HIS 产生新的 snapshotVersion。
- 同次分析固定一个快照及分析截至时间；住院历史病程不能当作当前状态。
- 所有工具响应保存来源版本和获取时间。HIS 无一致性快照能力时明确标记，不承诺读到原子快照。
- 补查先完成再冻结专家分析快照；分析中发现来源更新则终止当前分析并提示重新运行，不静默混合版本。
- 不连续轮询 HIS；初次分析和医生显式刷新时检查版本。无法获知未刷新期间的上游变更时，不展示“实时最新”声明。
- 报告绑定 snapshotVersion；快照变化后旧报告标记 STALE，旧审核保留但不代表新报告获批。
- 事实、来源或记忆更正同样使相关摘要和上下文失效；容量未到压缩阈值也必须处理。原始消息保留审计，但重建上下文时不能把其中旧结论继续作为当前事实。

## 4. 多智能体与技能运行规则

### 4.1 Agent 定义
| agentId | 角色 | 职责/工具范围 |
|---|---|---|
| assistant | 接诊分流助理 AI | 类似客服接待：按自身技能获取主诉和 HIS 资料、识别风险及分流缺项，匹配专家并交接；不替代专家辨证 |
| doctor01_zhang | 六经辨证学派 AI | 在经审核的症状适用范围内担任主诊或会诊，执行自己的四诊 SOP；HIS 补查＋RAG 只读工具 |
| doctor02_spleen | 脾胃/脏腑辨证 AI | 在经审核的脾胃症状适用范围内担任主诊或会诊，执行自己的四诊 SOP；HIS 补查＋RAG 只读工具 |
| summary | 汇总节点 | 汇总已获得的观点与证据，不增加未核实事实，不持有写入工具 |

专家名称明确标识 AI；不能暗示真实名医本人参与。两名专家是起步配置，不等于覆盖所有中医专科。

Agent 注册配置至少包含 `agentId/displayName/specialties/school/symptomTags/routingRules/exclusionRules/supportedPopulation/supportedPageSources/skillName/skillPath/soulPath/toolAllowlist/enabled/configVersion`。症状范围、排除条件和人群规则由医学人员审核；不能仅凭专家名称或模型自报“擅长”分配病例。每次运行记录配置、技能与模型版本。

### 4.2 Skills 装载
- 使用受控 SkillRegistry 和 SkillsAgentHook，模型通过 `read_skill(skill_name)` 读取完整 SKILL.md；这属于技能级披露，不假定框架原生支持按 stepId 读取章节或执行 YAML 状态机。SKILL.md 的 name、注册配置 skillName、read_skill 参数及使用 groupedTools 时的键必须一致，agentId 不必与技能名相同。
- SOUL.md 由应用显式加载，不假定框架自动识别。
- 技能定义：适用范围、前置资料、专家特有的四诊关注点、按序 SOP 步骤、每步工具及触发条件、补充问题、输出格式、完成条件和终止条件；不得仅包含角色介绍。
- 每位专家必须绑定独立且可读取的 SKILL.md；公共四诊/风险资料允许按版本引用，但不能让所有专家仅共用一份通用 SOP。分流助理也有自己的接待与转交技能，不加载全部专家 SOP。
- 一期路径映射：`assistant → agents/assistant/SKILL.md`（待新增），`doctor01_zhang → agents/doctor_zhang/SKILL.md`（已有待调整），`doctor02_spleen → agents/doctor_spleen/SKILL.md`（待新增）；路径相对 `src/main/resources/`，由注册表配置，不根据 agentId 猜目录。此处是开发要求，本次文档补充不创建这些文件。
- 每位专家的 SKILL.md 保留独立 SOP 主干、执行契约和章节索引；长篇医学说明放入该技能的 references/。主文件尽量保持约2000 token，但不得为缩短文件删除必要契约；按实际 token 验证。模型先读取主文件，再通过第4.13节受控本地工具按需读取当前步骤扩展章节，不将整份技能读取冒充章节读取。
- 技能、SOUL、契约、references 和获准公共资料按同一发布清单固定版本及内容哈希。启动时校验唯一 stepId、阶段顺序、章节映射和工具；路径越界、文件缺失、未知工具或不一致配置阻止启用。
- 一期从打包资源装载，只注册该 Agent 获准的技能；可适配 ClasspathSkillRegistry，但必须测试可执行 JAR。若解包，使用应用专用、版本隔离目录并校验哈希，不扫描默认用户技能目录，不接受用户级同名覆盖。禁用 autoReload；一个业务 run 内全部 Agent 使用开始时固定的技能清单。
- 配置工具名必须映射到实际 ToolCallback/MCP 能力；启动校验缺失映射并阻止对应 Agent 启用。
- 技能读取成功且本次输入包含必要技能正文后才允许暴露相关工具；仅表示内容已交付，不以模型自报“已读”作为证明。groupedTools 的技能激活状态不等于步骤权限；每次请求重新求工具交集，步骤结束立即撤销旧步骤权限，执行前再次校验。
- 不提供 Shell、任意文件访问、任意 URL、SQL、知识发布或 HIS 写入工具。
- 病历和检索文本是外部数据，不能覆盖系统指令或动态改变工具白名单。

现有 `src/main/resources/agents/doctor_zhang/{SOUL.md,SKILL.md}` 与 `agents/common/` 需要后续调整：取消完整重复问诊、默认脉象缺失、绝对确定结论、方药剂量与自动随访；医学内容经临床人员审核后方可用于真实诊疗。

### 4.3 分诊与会诊
助理输出固定结构：`primaryAgentId/consultantAgentIds/reason/factIds/missingInformation/outOfScope/normalizedSymptoms/candidateAgents`。标准化症状保留原文、存在/否认/未知状态及来源引用；候选专家包含匹配规则及不覆盖的问题。最终 routingStatus 和专家分配由程序校验后产生，模型输出不是授权结果。
- 程序校验专家存在、启用、症状适用范围、人群/场景限制、排除条件、工具权限和会诊数量；不接受模型自行创建的新 Agent。
- 没有适合专家时返回 OUT_OF_SCOPE，不强制分配；必要分流信息不足时返回 NEEDS_INFORMATION。医生可从符合当前适用范围的已启用专家中指定目标，不能绕过风险、授权或排除条件。
- 默认一个主诊，至多一个会诊，串行执行；需要更多专家属于后续扩展。
- 各 Agent 消息历史独立，共享不可变事实和授权证据；专家观点不能被其他专家写成患者事实。外层 Graph 只传递经过校验的交接对象和结果引用，不传父级完整 messages；各专家使用独立 outputKey。若使用框架子 Agent 适配，显式验证 includeContents(false)、returnReasoningContents(false) 的1.1.2.2语义，不能依赖默认历史共享或仅靠开关替代访问控制。
- 冻结前任一已分配专家缺少必需资料时，整轮返回 NEEDS_INFORMATION，不跳过其准备阶段；不得默默移除会诊来宣称资料齐全。冻结后会诊非安全性执行失败可保留已完成主诊意见，报告标记 PARTIAL，明确缺少哪位专家。风险、授权或来源失效仍按全轮终止规则处理，不能用 PARTIAL 绕过。
- 多 Agent 共用一个本地模型是允许的；意见一致不代表独立医学验证，不输出投票式确诊或模型自报诊断概率。

### 4.4 标准分析流程
```text
校验会话与操作者权限
→ 助理加载技能
→ MCP 核验就诊并获取病历
→ 事实整理、必要数据校验和风险筛查
→ 助理依据主诉症状匹配专家，程序校验并形成结构化交接
→ 主诊加载独立 SKILL.md，执行 PREPARE 阶段，核对四诊和补查缺项
→ 已分配会诊加载自己的 SKILL.md，执行 PREPARE 阶段（如有）
→ 程序复核全部参与专家的必要资料、来源、风险与适用范围
→ 固定获准历史记忆及版本，冻结唯一共享分析快照
→ 主诊执行 ANALYZE 阶段，四诊合参并按 SOP 检索支持/反向证据
→ 会诊基于同一快照执行自己的 ANALYZE 阶段（如有）
→ 汇总结构化意见、分歧和实际证据
→ 输出 Schema、来源、风险与范围校验
→ 形成待补充或待审核报告
```
- 外层 StateGraph 由程序推进分流、准备、冻结、辨证与汇总；SopExecutionCoordinator 负责状态转换校验，数据库是业务状态权威，Graph 不是另一套独立状态机。内层 ReactAgent 只在当前步骤许可范围内推理和调用工具，不由模型决定是否跳过风险检查或冻结。
- 助理使用结构化推荐＋程序校验，不直接采用按 description 自动转交的默认 LlmRoutingAgent；一期不以 SupervisorAgent 自由循环承担整个诊疗流程，也不引入可执行脚本式通用 SOP 引擎。
- PREPARE 与 ANALYZE 是同一专家 SOP 的两个阶段；阶段间暂时返回外层图，不新建业务 run、不重置该专家预算。阶段重入从当前已提交步骤继续，仅适用于未中断的本轮；进程中断仍创建新 run 重检。
- 冻结前将新资料导致的已完成准备步骤标记待重检；只复用仍匹配来源版本和覆盖范围的取数记录，不无条件重复 REQUIRED 调用。所有步骤计入同一总预算。冻结后不增配专家；新会诊请求创建新 run 并重新经过准备阶段。
- 必要病历没有读取成功，不允许产生声称基于该病历的辨证报告。
- 每次出现新患者数据都执行风险筛查；规则与模型辅助结合，未触发不能理解为排除急症。
- 紧急风险时进入 RISK_ESCALATED，优先提示当前诊疗团队核实处理，不自动生成治疗方案；住院不能机械提示“到医院就诊”。
- 明显缺少影响判断的信息时输出 NEEDS_INFORMATION 及问题清单，可保存受限阶段性报告。
- RAG 查询返回相似片段不等于证据支持；专家须说明适用性、反向证据和差异。
- 冻结专家分析快照前，应用加载获准使用的历史记忆并固定其 ID/版本及记忆范围 epoch；每次模型调用前经过第4.8节上下文守卫，报告提交前再次检查有效性。

### 4.5 记忆分层与权威边界
| 层次 | 内容 | 存储与使用规则 |
|---|---|---|
| 原始业务记录 | 消息、事实快照、工具资料、报告、审核 | MySQL 分页持久化；用于审计和重建，不全量回放到模型 |
| 短期记忆 | 当前任务、近期完整交互、阶段摘要、本轮工具状态 | MySQL 保存可恢复内容，Graph 保存单次 run 工作状态 |
| 长期记忆 | 已确认历史临床事件、就诊摘要、医生显示偏好 | MySQL 保存权威内容/来源/版本；Mem0 提供受控语义检索 |
| 医学知识 | 文献、专家经验、独立审核后的相似病例 | 现有 RAG 服务；不能把私人会话记忆自动发布为公共知识 |

短期/长期指使用范围，不指物理存储介质。跨会话记忆是按授权检索长期记录，不复用其他会话的模型线程；同一患者的纵向历史也不是“相似病例”。历史存储受机构配额和留存策略约束，不承诺无限；上下文压缩不删除业务原文，不等于模型训练或自动修改 Skills。

### 4.6 短期记忆与会话恢复
- 业务会话归属 `organizationId＋sessionId`；专家上下文归属 `organizationId＋sessionId＋agentId`，绑定 snapshotVersion。原始消息按会话分配单调 seq，并记录可见范围和来源关联。
- 每个 `runId＋agentId` 使用新的不透明 threadId，由服务端生成映射；不复用其他 run 的未完成工具状态。新运行通过 MySQL 消息、有效摘要、当前快照和获准历史重建输入。
- 一期 Graph/checkpointer 只承担单次运行的工作状态，不能作为跨实例会话唯一存储；运行结束释放其内存状态。崩溃按第11节失败处理，不承诺从模型思考或工具循环中间自动续跑。
- 只保留该 Agent 可见的近期完整交互；医生消息的目标范围由应用分发。专家之间只传递结构化意见和证据引用，不共享完整历史，不保存或传播隐藏思考过程。
- 会话摘要保留已确认事实引用、更正、讨论目标、带标签的候选/假设/分歧、待回答问题和来源。不能把未记录改写成否认、历史改写成当前、假设改写成事实。
- 摘要绑定 `snapshotVersion/coveredThroughSeq/sourceMessageIds/sourceRefs/summaryVersion/modelVersion`；原始消息不变。按固定消息水位生成，写回用上下文 version 做 CAS，旧任务不得覆盖较新摘要。
- 摘要可递增生成，但保留分段与原文引用；发现摘要冲突、来源更正或快照变化时，从原始记录/可信结构化事实重建，不能无限只对上一份摘要再摘要。
- 原文回取走会话授权接口和受控资料记录，重新验证来源状态；不能通过任意文件路径或 URL 读取。旧资料仅供审计时不得作为当前分析材料。

### 4.7 长期及跨会话记忆
| scope | 归属与可保存内容 | 读取边界 |
|---|---|---|
| ENCOUNTER | 机构＋患者＋就诊：医生显式确认的阶段摘要 | 同就诊其他会话可引用，不复制私人聊天 |
| PATIENT | 机构＋患者：有来源且明确确认的历史临床事实/事件 | 跨就诊按授权引用，明确标记历史，不自动成为本次事实 |
| DOCTOR | 机构＋医生：明确确认的展示偏好 | 仅该医生可用；不含患者内容，不影响医学判断和权限 |

- scope 和 subjectKey 由服务器构造；患者/医生用不同用途的不透明标识或带用途及密钥版本的机构内 HMAC，不能混用 Mem0 user_id，也不能用身份证或姓名做关联。相同 HIS patientId 不代表跨院同人。
- 同患者不同医生，只共享有权访问的临床记忆，不共享原医生私人讨论和偏好；同医生不同患者只复用医生偏好；一期禁止跨机构记忆共享。
- 仅经第7.4节显式确认的临床事实、具体诊断、就诊摘要或显示偏好可成为 ACTIVE 记忆。报告 APPROVED 不等于全部候选确诊；普通回答、假设、摘要压缩结果不自动写为长期事实。
- 记忆具有来源引用、事实确认依据、发生时间 occurredAt、系统获知时间 recordedAt、适用期间 validFrom/validTo、内容 revision 和状态。事件结束不抹去历史，但不能继续按当前状态使用；相互矛盾的来源进入 CONFLICT，不能简单“最后写入者获胜”。
- 新会话先核验当前 HIS，就诊历史通过确定性读取和受控语义检索补充。过敏、重要风险等关键资料及医生显示偏好优先结构化读取，不依赖向量 topK 是否命中。
- 回顾分析不能引入 analysisAsOf 之后才发生或获知的材料；有效历史事件与当前仍适用事实分开标注。历史与当前冲突时要求医生核实，不按相似度选择真相。
- Mem0 搜索必须先附加服务器确定的机构、主体和范围过滤；命中后批量回查 MySQL，复核访问权、当前 revision、确认状态、来源、时间和 synthetic 标记，注入权威正文而不是直接采用向量载荷。
- 本轮固定记忆范围 epoch 和所用版本。后续披露或提交前发现 epoch/来源变更则终止为 NEEDS_INFORMATION，errorCode=STALE_CONTEXT，提示重新运行；不在同次分析中混用新旧记忆。
- 同会话活动运行锁之外，记忆写入还锁定 scope 行并校验记忆记录 version，递增 epoch。相同 scope＋memoryKey 具有唯一业务记录；并发创建和更正冲突返回409，不静默覆盖。

### 4.8 渐进式披露与压缩执行
每个 Agent 独立计算预算，不能将多个 Agent 的窗口相加；下面流程覆盖主诊、会诊、助理和汇总的每次模型调用，而非只在医生输入时执行：
```text
读取本轮快照、记忆 epoch 和消息水位
→ 检查来源/权限/版本并整理失效内容
→ 组装固定规则、当前问题、关键事实、摘要、近期交互、相关记忆和证据
→ 对即将装载的 Skill、工具 schema 和资料预估 token
→ 去重并按阈值压缩已完成历史
→ 物化当前技能、步骤工具集、结构化输出指令与模型参数
→ 最终请求守卫重算输入，检查硬上限、消息结构和保护项
→ 申请模型许可；等待后复核授权、版本、取消状态与剩余期限
→ 原子预占本次调用预算，通过统一模型适配层发送
→ 工具返回后保存受控资料，重新进入守卫
```
- 检查触发点：新消息、Skills 装载前后、HIS/RAG/Mem0 返回后、专家交接、Graph 每次调用前，以及快照/来源/记忆失效。失效触发与 token 阈值无关。
- 披露按“目录/用途→有关章节/片段→需要时原文”执行；同来源＋版本＋片段哈希去重。历史工具响应不无限追加，完成阶段保留结构化结果、必要原文引文与可回取引用。
- 当前技能必需约束、系统安全规则、当前问题、关键临床事实、否认/冲突/风险状态及仍需要的证据是保护项。固定系统规则与外部资料分离，摘要和记忆不得提升为系统指令。
- 记录 `loadedSkillVersion/activeSections/sourceRefs` 仅表示装载历史；内容移出窗口后，下一步需要时必须重新读取。当前动作必要的技能内容未在输入中时不得仅因“曾读过”继续执行。
- 未知工具返回长度使用分页和输出上限约束，返回后重新计量；完整正文保存在受控业务资料中，输入只取必要部分。资料不完整必须显式标记，不允许静默截断后声称完整。
- 压缩顺序：去重及移除失效/无关材料→用可信结构化事实替代冗余原文→摘要较早且已结束的交互→按需回取细节。不能通过删掉保护项满足目标压缩率。
- 一次交互包括用户输入、相关 assistant 工具调用、全部对应 tool 结果和最终回答；按完整单元裁剪，保留未完成工具链，不留下孤立 tool 消息。
- 70%触发软压缩，一期在调用前同步执行；85%起暂停继续扩张输入并强制整理。每次检查至多一次摘要尝试，不足目标但低于85%且覆盖完整可继续；仍达85%且无法安全缩减则 NEEDS_INFORMATION/CONTEXT_BUDGET_EXCEEDED，不反复循环压缩。
- 超过输入硬上限不得发送模型请求。达到85%/100%触发值应分别按第12.4节的不等式处理，不依赖服务端自动截断。
- 摘要模型也受自身 token 上限约束；超大历史按消息单元分段。每个摘要模型请求消耗全轮调用/时间预算，不允许递归调用压缩 Hook。摘要失败或摘要预算不足时丢弃失败结果并记录 CONTEXT_COMPACTION_FAILED；只有确定性整理后的工作集保留全部保护项、引用有效、工具链完整、C低于85%输入预算且仍有主模型调用/时间预算时才可降级继续，否则以 NEEDS_INFORMATION 终止。
- 一期不启动独立后台摘要模型争用同一许可；先压缩、后申请主模型许可。不得持有主模型许可或数据库长事务等待摘要，以免单并发死锁。

### 4.9 Mem0 受控接入与故障降级
- 采用 OSS 适配服务，不直接向 Agent 暴露 Mem0 原始管理 API。先用合成病例验证本地模型、过滤、更正、重试和性能，再打开 `tcm.memory.semantic-enabled`；关闭时仍有完整短期记忆和结构化历史能力。
- 医疗记忆采用应用先确认、适配服务直接索引的模式，优先验证锁定版本的 `infer=False`。若启用自动抽取，其结果仅为待确认候选，不自动进入 ACTIVE；不依赖 SDK 自动处理医学矛盾。
- 记忆版本、scope epoch 和 `tcm_memory_sync_job` 在 MySQL 同一事务提交；网络调用在事务外。UPSERT/REVOKE 投递键为 `organizationId＋memoryId＋revision＋operation＋generation`，同键使用稳定 eventId；跨代次重建生成新任务和新 eventId。接收端同时约束业务投递键及机构＋eventId唯一，校验覆盖全部业务载荷的 payloadHash，同键不同载荷返回409；contentHash仅表示记忆正文哈希。
- 适配服务先持久化接收任务和回执，再执行 SDK 索引；外部 ID 映射绑定机构、memoryId、revision和generation。最新修订/撤销屏障按机构＋memoryId跨代次生效，不能因新建代次接受已失效的旧修订。回执丢失后能查询并恢复，同一任务串行领取；SDK 内容去重不等于业务幂等。
- 更正先切换 MySQL 权威版本或撤销状态，并递增 epoch；随后异步更新投影。即使旧向量尚存在，回查也不能通过；迟到的旧 UPSERT 不得越过新修订/撤销屏障。读取、索引、删除失败不得自行恢复旧权威状态。
- 记忆投影具有独立 generation，记录 Embedding 模型、revision、维度和预处理版本；模型迁移重建后切换，不能用新向量查询旧空间，不能与 RAG 共用代次指针。重建期间更正/撤销需同步到活动及重建代次，切换前追平变更并复核当前权威状态；跨代次屏障只控制有效性，不能将另一代次的成功回执当作本代次已完成索引。
- Mem0 超时或关闭时，使用 MySQL 的有效摘要、近期消息、当前事实及有界结构化历史；返回 SEMANTIC_MEMORY_UNAVAILABLE 提示，不解释为患者没有历史。MySQL/授权校验失败必须关闭访问，不以缓存绕过。
- 会话关闭不自动批准记忆，也不删除医疗原始记录。记忆撤销立即禁止新分析使用，投影/缓存清理可异步；医疗原件、审计、备份的留存或删除按独立政策执行，不承诺物理数据即时全部清除。

### 4.10 代码组件职责与接入点
以下为待实现组件契约，不表示框架已有同名 API；以项目锁定版本编译验证 Hook/Interceptor 的接入方式。
| 组件 | 位置 | 核心职责 |
|---|---|---|
| ContextAssembler | agent/context | 按当前 run、Agent、快照、水位及授权组装 ContextEnvelope，不全量回放 |
| TokenBudgetPolicy / ModelTokenCounter | agent/context | 计算有效窗口、各分项和最终输入；匹配模型 tokenizer/chat template |
| ContextCompactionService | application/memory | 去重、保护项提取、分段摘要、CAS 发布；原文不变 |
| ContextGuardHook | agent/context | 通过 ModelHook/MessagesModelHook 整理状态和成组消息、触发受控压缩；不是最终请求的唯一守卫 |
| FinalRequestGuard / BudgetedModelGateway | agent/context / infrastructure/model | 检查物化后的实际请求，统一模型许可、调用预算及剩余期限；摘要和格式化调用同样受控 |
| StepToolPolicyInterceptor / AuthorizedMcpToolCallback | agent/sop / infrastructure/mcp | 请求侧筛选步骤工具，执行侧再次鉴权、签名、审计与取消检查，禁止绕过包装器 |
| ReferenceStatusGateway | infrastructure/rag | 无模型地调用内部引用状态接口，供报告展示及提交前复核 |
| DisclosureCoordinator | agent/context | Skills/工具响应分页、重复装载去重、原文回取及按需替换 |
| MemoryApplicationService | application/memory | 显式确认、更正、撤销、scope epoch 并发控制和同步事务 |
| MemoryQueryService | application/memory | 确定性历史查询、Mem0 召回后的权威复核、受限降级 |
| SemanticMemoryGateway / Mem0RestAdapter | domain/memory / infrastructure/memory | 定义 search/upsert/revoke/receipt 应用契约，屏蔽 SDK 参数差异 |
| MemorySyncWorker | infrastructure/memory | 领取数据库租约任务、幂等投递、回执确认、退避和修订屏障 |
| ExpertRoutingService | application/routing | 基于注册表及已核验症状筛选候选，校验助理推荐/医生指定并记录交接依据 |
| SkillContractValidator | agent/skills | 读取每个独立技能的步骤契约，校验版本、路径、完成条件和工具映射 |
| SopExecutionCoordinator | agent/sop | 维护当前步骤，约束步骤工具集、检查证据及前置条件，发布步骤状态；不把 Markdown 当可执行脚本 |

- `ContextEnvelope` 包含 runId、agentId、snapshotVersion、scopeEpochs、messageWatermark、summaryRefs、memoryRefs、sourceRefs、skillVersions、分项 token 和 completeness；敏感正文只进入受控业务记录和授权模型调用，不写调试日志。
- 原始业务历史只有一套写入路径。禁止 Graph 消息、ChatMemory Advisor 和自定义组装器重复追加同一历史；本期由 ContextAssembler 重建每个 run 的输入，Graph 仅累积该 run 内的工具交互。
- 压缩前后记录输入哈希与 token 数；同水位/快照/规则版本且未新增内容时复用有效摘要，不因重复 Hook 无限生成摘要。调用前和报告提交前均验证 scope epoch 与引用状态。

### 4.11 客服式接诊与症状分流落地
1. 助理接收当前会话的主诉和症状；先核验就诊并获取必要 HIS 资料。入口未核验主诉仅可用于初步了解，不作为已确认诊断；假设、否认及既往症状不能当作当前阳性症状匹配。
2. 执行风险筛查，存在紧急风险先进入 RISK_ESCALATED，不继续常规专家分流。助理只追问影响分流的必要信息，完整四诊由被分配专家负责，不要求分流前完成全部四诊。
3. 应用按注册表筛选启用且当前有权使用的专家，先应用人群、门诊/住院范围及排除规则，再用有来源的症状匹配专长。规则必需信息未知时要求补充，不能默认满足；症状归一结果是解释性派生信息，不自动改写患者事实。
4. 助理仅在候选集合中建议一位主诊和至多一位会诊，并说明主诉对应的匹配规则与未覆盖症状。不能按学派名称直接分配，不能只因某个词命中就认定能诊治；多主诉无法选出明确主问题时先请医生明确，不循环转交多个 Agent。
5. 程序通过校验后分配，固定 `routingConfigVersion/primaryAgentId/consultantAgentIds`，向专家交接当前主诉、factIds/sourceRefs、风险、缺项、分配理由及已完成取数记录，不交接助理隐藏思考或全部私人对话。分流状态为 MATCHED、NEEDS_INFORMATION、OUT_OF_SCOPE、RISK_ESCALATED；除 MATCHED 外不进入常规专家 SOP。
6. 补查改变症状或出现排除条件时，冻结快照前重新核验分配；不适用则结束本轮并提示新建分析运行，禁止无界转诊。冻结后来源变化遵循第3.3节终止规则。普通追问沿用原主诊；新增事实后重新检查适用范围，不因措辞变化每轮换专家。

例如，合成病例“胃脘不适、腹胀”在满足脾胃专家已审核的适用规则、且没有风险或排除条件时，可交给 doctor02_spleen；这仅是路由测试示例，不是临床分诊标准。六经专家的覆盖范围也须显式配置，不能当作所有不匹配症状的兜底专家。

### 4.12 每位专家独立四诊 SOP 与步骤级工具
SKILL.md 是专家诊疗流程的说明与受控步骤契约，不是自动执行 Markdown 的脚本。每位专家须说明自身的询问重点、四诊合参方法、鉴别要点和证据要求；共用下列阶段不等于共用全部医学 SOP：

| 阶段 | 专家 SOP 行为 | 按需工具/推进条件 |
|---|---|---|
| 接收病例 | 核验交接、专长适用性、已有资料和风险 | 复用本轮有效取数结果；缺少必要病历时用 get_current_medical_records，不重复取相同版本 |
| 四诊核对 | 望：已记录的形色/舌象；闻：声音/气味记录；问：主诉病程及伴随症状；切：脉象/触诊记录 | get_current_clinical_data 的 FOUR_DIAGNOSIS；按需合并 HISTORY/MEDICATIONS/ALLERGIES 等允许类别，不能声称模型亲自望闻切诊 |
| 缺项补充 | 仅针对该专家判断所需的缺失或冲突项提问 | HIS 无可用记录时输出待医生核实问题；不能以反复调用同一工具代替人工采集 |
| 四诊合参 | 全部参与专家准备完成、应用冻结共享快照后，按本专家学派形成候选及鉴别 | 依据事实引用，区分未知与否认；缺少关键资料时输出 NEEDS_INFORMATION，不虚构舌脉 |
| 证据核验 | 检索支持/反向文献，必要时比较病例及读取原文 | knowledge_mcp；find_similar_cases、read_reference 按需调用；无匹配不伪造证据 |
| 结果交付 | 汇总辅助辨证、分歧、缺项及后续核实问题 | 通过报告 Schema/来源/风险检查后交医生审核；实际治疗由医生决策，不产生处方、剂量或医嘱 |

步骤契约采用 SKILL.md 中唯一的“执行契约”YAML 块，由应用读取为白名单数据结构；根结构包含 `schemaVersion/agentId/skillVersion/sections/steps/explanationStep`，sections 为章节白名单，steps 是正式分析按执行顺序排列的列表；助理和专家必须另有 explanationStep，结构与普通步骤相同，但不放入 steps。stepId 在两者之间也必须唯一。框架技能元数据仍按1.1.2.2要求提供。每步必填 `stepId/phase/instructionRefs/title/goal/prerequisites/toolPolicy/allowedTools/inputRequirements/outputRequirements/completionCheck/onMissing/onFailure`；required 默认 true，仅 required=false 的步骤可配置 applicabilityCheck 判定不适用。IF_NEEDED 必须指定 toolNeededCheck；可选 toolArguments 只允许相应工具 schema 的非保留参数。枚举及检查项映射到 Java 已注册策略，禁止脚本、任意表达式和模型改写执行契约。

以下仅展示一个步骤片段，不是完整技能文件；完整 SOP 必须包含上述全部必要阶段及专家自己的医学流程说明：
```yaml
stepId: collect_four_diagnosis
phase: PREPARE
instructionRefs: [four_diagnosis_inventory]
title: 核对本次四诊资料
goal: 整理已有望闻问切记录，标识缺失与冲突
prerequisites: [ENCOUNTER_VERIFIED, RISK_SCREEN_PASSED]
toolPolicy: IF_NEEDED
toolNeededCheck: NO_REUSABLE_CURRENT_FOUR_DIAGNOSIS_RESULT
allowedTools: [get_current_clinical_data]
toolArguments:
  categories: [FOUR_DIAGNOSIS]
inputRequirements: [AUTHORIZED_ENCOUNTER, CURRENT_SOURCE_REFS]
outputRequirements: [FOUR_DIAGNOSIS_FACTS, MISSING_ITEMS, SOURCE_REFS]
completionCheck: FOUR_DIAGNOSIS_INVENTORIED
onMissing: NEEDS_INFORMATION
onFailure: STOP_WITH_DEPENDENCY_ERROR
```
- `toolPolicy` 仅允许 NONE、IF_NEEDED、REQUIRED。NONE 不暴露 MCP 工具；IF_NEEDED 由已注册的 toolNeededCheck 检查是否需要调用，否则复用已核验的同版本结果并记录依据；REQUIRED 必须有本轮该步骤的实际成功调用记录，不能仅凭模型说“已查询”推进。记录缺项不等于获得完整四诊，进入合参前仍需通过专家必需资料检查。onMissing 处理本步骤必需资料缺失；资料盘点可以包含未记录项，是否阻断由已审核的专家必要项规则决定，不强制补齐所有四诊字段；权限失败和紧急风险优先于步骤降级策略。
- 实际可调用 MCP 工具是“已注册能力∩操作者授权∩Agent 白名单∩当前步骤 allowedTools∩运行阶段限制”的交集，还须通过技能内容就绪检查；仅 IF_NEEDED 使用 toolNeededCheck，REQUIRED 按注册完成策略验证实际调用。每步可以声明工具，但不是每步必须调用；模型生成 tool_calls，Java 校验并执行。read_skill/read_skill_section 属于独立受控本地能力，不冒充 HIS/RAG MCP 工具。
- 正式分析 steps 的 phase 仅为 PREPARE/ANALYZE，所有 PREPARE 步骤先于 ANALYZE；HIS 取数只能配置在 PREPARE。接收病例、四诊核对、缺项补充属于 PREPARE，四诊合参、证据核验和交付属于 ANALYZE；同一步骤不能跨越冻结点。explanationStep 固定 stepId=explain、phase=EXPLAIN、required=true，只能用于 EXPLANATION 运行，不能混入 ANALYSIS 的四诊步骤序列。
- 步骤转换由应用检查前置条件、来源、输出结构及完成条件，模型不能自行跳过未完成的必需步骤。四诊整理可以一次工具调用读取多类别；步骤数不等于模型调用数，全部调用仍受第12.2节预算和第4.8节上下文守卫约束。
- 冻结后不再暴露获取新患者事实的 HIS 补查工具；发现缺项/新资料按既有规则结束当前分析并提示重新运行。文献检索及原文读取仍按当前步骤和授权执行。
- 步骤状态为 PENDING、RUNNING、COMPLETED、SKIPPED、BLOCKED、FAILED；仅 required=false 且 applicabilityCheck 判定不适用时可记 SKIPPED，须保存理由；必需步骤不能跳过。工具失败不等于步骤完成，终态错误沿用 DEPENDENCY_UNAVAILABLE / DEPENDENCY_TIMEOUT / MODEL_OUTPUT_INVALID。
- 需要医生补录时，将步骤记为 BLOCKED，本轮返回 NEEDS_INFORMATION 并释放执行资源；医生用 FACT_SUPPLEMENT 显式确认，生成新快照和新 run 后重新检查步骤条件，不把旧 run 当作自动恢复。知识性追问仍是 QUESTION，不能自动写入事实。
- 运行绑定技能版本及内容哈希，步骤审计关联 runId、agentId、stepId、skillVersion、来源与工具 callId。运行中不热换 SOP；新版本用于新 run，若旧版本被停用则终止其继续调用，不静默切换。压缩后需要的当前步骤内容必须重新加载。

### 4.13 Spring AI Alibaba 1.1.2.2 接入契约
以下是应用实现要求，框架类型是待以1.1.2.2源码核验的接入点，不宣称网页示例已在本工程运行。

| 能力 | 框架接入点 | 本项目必须实现的边界 |
|---|---|---|
| 外层编排与专家任务 | StateGraph、ReactAgent | 程序决定阶段；模型返回最终回答不等于 SOP 完成，阶段产物校验通过才推进 |
| 技能发现与主文件读取 | SkillRegistry、SkillsAgentHook | 每 Agent 独立受控注册表，read_skill 读取主文件；自有契约解析器执行阶段与完成规则 |
| 历史替换与压缩 | MessagesModelHook / ModelHook | 显式使用替换语义并保持工具消息链；摘要先经保护项检查和 CAS，不启用第二套默认摘要逻辑 |
| 动态提示与工具披露 | ModelInterceptor | 每次请求求当前工具交集，加入必要技能与输出指令；最后执行 FinalRequestGuard |
| 工具执行 | ToolInterceptor、包装后的 ToolCallback | 每次执行和重试前复核身份、步骤、版本、取消状态；注入签名，拒绝未包装回调 |
| 模型预算 | ModelCallLimitHook＋统一模型适配层 | 内置限制仅作为单次 Agent 执行附加保护；业务 run/专家跨阶段总预算由应用统一计数 |
| 结果隔离 | outputKey、显式子 Agent 输入适配 | 只交接校验后的结构化结果；不复制父消息、私人讨论或隐藏思考 |
| 运行上下文 | RunnableConfig、运行期 checkpointer | 同一 run/Agent 两阶段使用其隔离上下文；结束释放，跨实例从 MySQL 重建，不自动恢复中断工具循环 |

技能章节与工具控制：
- sections 每项包含 `sectionId/resourcePath/headingId/contentHash`。路径只来自已校验清单，映射 SKILL.md、该技能 references/ 或明确获准的公共资料；instructionRefs 引用 sectionId，标题锚点必须唯一。公共资料版本也固定在清单内。
- 新增本地工具 `read_skill_section({sectionId})`：应用从当前 run/Agent/step 确定技能版本，只允许读取本步骤 instructionRefs；返回正文、版本和哈希，不接受任意路径、URL 或模型自报技能版本。超出当前预算则拒绝或停止，不截断后声称完整。主文件已包含对应正文时可直接登记其覆盖范围。
- 系统规则与当前步骤必需约束始终进入受保护输入；扩展正文移出窗口后，下一次使用前重新装载。技能“已激活”不代表内容仍在当前请求中。
- 不将所有 MCP 工具注册成不受限制的默认工具。1.1.2.2 中空 tools、默认回调、动态回调及 groupedTools 的合并行为须在 D2 核验；禁止以 `tools=[]` 推定全部禁用。NONE 的验收依据是最终请求确实没有 MCP schema，伪造调用也无法到达 MCP Server。本地技能读取及经验证的输出格式化工具按各自白名单单独管理。
- 一期同一模型响应中的工具调用串行执行，每个调用及重试单独校验和审计。整批返回处理完成后才推进步骤；前一个结果触发风险、失效或取消时，剩余调用不执行，按工具协议记录拒绝结果或终止运行，不留下可继续发送的孤立工具链。

请求执行顺序：
1. Before Model Hook 检查业务状态、整理消息并按规则压缩；不能只使用每次 Agent 调用一次的 BEFORE_AGENT 充当循环守卫。
2. Model Interceptor 装载必要内容并确定步骤工具、模型选项和结构化输出约束；FinalRequestGuard 位于所有会改变输入的处理之后。若框架还在更下游追加 schema，模型适配层必须补做物化检查，不能凭 Hook 调用次数推定覆盖。
3. 请求包含系统提示、instruction、消息、技能、实际工具 schema、输出格式指令及 chat template 后才计量。检查后不得再增加未计量输入；无法证明顺序时阻止推理就绪。
4. 统一入口申请许可并校验剩余期限，发送前预占调用次数；失败和超时同样消耗次数。摘要不持有主模型许可，格式化、纠正和框架额外调用也不能绕过入口。
5. ToolInterceptor 执行侧再次校验；来源/权限失败是业务终止，不伪装为普通成功文本。模型结束、达到迭代上限、预算终止分别映射业务状态，不能都视为 SUCCEEDED。

### 4.14 工具结果与步骤完成判定
完成检查采用已注册策略；“工具执行完成”“资料覆盖满足”“医学证据支持”分别记录，不能互相替代。
| 情况 | 调用与步骤的判定 |
|---|---|
| OK 且资料覆盖满足 | 仍须通过来源、输入输出和 completionCheck；才允许完成步骤 |
| PARTIAL 或仍有 nextCursor | 核对必要类别和范围是否已覆盖；未覆盖则在预算内继续分页，否则标记缺项，不能声称完整 |
| 文献检索 NO_MATCH | 可视为检索正常完成并满足 REQUIRED 的真实调用要求；记录证据不足，不等于支持候选；是否能交付受限结果由完成策略决定 |
| FORBIDDEN / INVALID_ARGUMENT | 不重试；分别按权限失败或参数错误终止，不能转为“没有病史” |
| UNAVAILABLE / 超时 | 仅按有界重试策略处理；不满足 REQUIRED，不能记录 COMPLETED |
| NOT_FOUND / STALE_SOURCE | 必需患者资料不存在则缺项；来源变更按失效规则终止，不复用旧正文 |
| IF_NEEDED 复用 | 核对机构、患者、就诊、时间、来源版本、类别与分页覆盖范围，并保存原始取数引用；复用不伪造新工具 callId |

allowedTools 是许可集合，不代表全部必须调用；required 调用集合及各项覆盖条件由对应 completionCheck 注册策略固定。数据缺项、NO_MATCH 和工具失败分别进入审计，不能只检查 HTTP 成功或模型自报完成。

## 5. MCP 工具契约

### 5.1 协议与执行边界
- tcm 维护 `his`、`rag` 两个 MCP Client 连接，模型只负责输出 tool_calls，实际执行在 Java 侧。
- 不同时启用 oMLX 内部 MCP 执行，避免双重调用。ReactAgent 工具循环是唯一执行入口；D2 验证 Spring AI ChatModel 内部自动工具执行不会绕过 Agent 拦截器，原始 MCP ToolCallbackProvider 不直接注入 Agent。
- 本节签名上下文仅用于活动 Agent 步骤；报告展示和提交前的系统引用复核使用第9.3节内部 REST，不创建假 Agent、假 stepId 或复用已结束步骤令牌。
- MCP 公共协议不保证 Java ToolContext 自动跨服务传输；必须实现应用层上下文封装和签名校验。
- 模型可见 schema 不包含患者、机构、权限、当前快照等字段。Java 包装器从运行状态注入保留参数 `_executionContext`，模型伪造该字段时拒绝，不透传。
- MCP Server 的实际 schema 将 `_executionContext` 声明为必填保留参数；tcm 对模型暴露裁剪后的 schema，执行前注入签名上下文，不能直接把未经包装的远端工具全部暴露给模型。
- `_executionContext` 是短期签名 JWS，包含 `iss/aud/sub/organizationId/runId/agentId/stepId/skillVersion/toolName/argumentHash/exp/jti`；agentId/stepId/skillVersion 由应用当前执行状态注入，不接受模型指定；argumentHash 绑定不含保留参数的实际调用内容。HIS audience 额外包含当前患者、就诊、场景和时间约束；RAG audience 仅含授权范围和去标识化病例排除标记。
- 令牌默认 60 秒有效、按工具调用签发；Server 验签、校验 audience/有效期/工具范围并再次授权。签名密钥外置，原始令牌不写日志。MCP Server 不信任未签名的自报身份。
- 工具返回内容在进入模型前做标识信息过滤；模型查询自由文本也检查身份证、电话等泄露。

### 5.2 HIS 只读工具
以下为模型可见参数；保留执行上下文由 Java 注入：
| 工具 | 输入 | 返回数据 |
|---|---|---|
| get_current_encounter | `{}` | 归属核验结果、必要人口学信息、场景、资料版本标识 |
| get_current_medical_records | `{documentTypes?: string[], cursor?: string}` | 当前就诊病历列表、原文分段、版本、时间、nextCursor |
| get_current_clinical_data | `{categories: enum[], cursor?: string}` | 指定类别的结构化临床数据及来源 |

`categories` 仅允许 VITALS、LABS、EXAMINATIONS、MEDICATIONS、ALLERGIES、HISTORY、FOUR_DIAGNOSIS；documentTypes 按门诊/住院白名单校验。分页游标是服务端生成的不透明值，绑定本次就诊。

FOUR_DIAGNOSIS 按 `inspection/auscultationOlfaction/inquiry/palpation` 分组返回第3.2节事实结构，分别对应望、闻、问、切，保留来源、时间、否认/未记录/冲突状态。HIS 已有记录直接核对，不要求患者重做四诊；未记录的声音、气味、舌象、脉象等由医生按需采集，本期不新增图像、音频或脉诊硬件识别工具。

模拟服务内置固定病例映射；未知患者、错误就诊、未知场景必须拒绝，不能回退到默认病例。病历超出模型预算时先分页读取和保留结构化关键项，仍不足则标记不完整，不静默截断后声称完整。

### 5.3 RAG 只读工具
| 工具 | 输入 | 用途 |
|---|---|---|
| knowledge_mcp | `{query: string, filters?: object, topK?: integer}` | 医学文献检索，query 1～2000 字符 |
| find_similar_cases | `{clinicalFeatures: object, filters?: object, topK?: integer}` | 基于已知症状、四诊、病史检索相似病例 |
| read_reference | `{referenceId: string, version: integer, chunkId?: string, cursor?: string}` | 读取已授权来源的具体片段或脱敏病例详情 |

- `filters` 只允许资料类型、专长、学派、发表时间范围；不允许指定机构、审核状态、SQL 或任意 metadata 表达式。
- `clinicalFeatures` 允许 `chiefComplaint/presentIllness/fourDiagnosis/relevantHistory/ageGroup/sex`，总长度不超过 8000 字符；未知项缺省，不包含身份信息。
- topK 默认 5、范围 1～10；非法值返回参数错误，不无界扩容。
- read_reference 不接受文件路径或 URL；每次最多 8000 字符，超长返回 cursor；重新验证来源发布状态和访问权限。
- 实际工具 schema 由服务统一维护，SKILL.md 使用相同名称和参数；MCP Client 导入的工具前缀由适配层统一映射。

### 5.4 返回结构
```json
{
  "schemaVersion": "1",
  "status": "OK",
  "data": {},
  "sources": [],
  "warnings": [],
  "retrievedAt": "2026-09-21T09:00:00+08:00",
  "traceId": "trace-example"
}
```
- 状态：OK、PARTIAL、NO_MATCH、UNAVAILABLE、FORBIDDEN、INVALID_ARGUMENT、NOT_FOUND、STALE_SOURCE。
- NO_MATCH 仅表示本次检索没有可返回资料，不表示医学上没有相关证据；服务失败用 UNAVAILABLE。
- sources 中每项含 `referenceId/sourceType/version/chunkId/title/locator/excerpt/synthetic/reviewStatus`；文献补充作者、发表时间及 DOI（有则提供），检索项提供 similarityScore。
- 未通过身份或协议验证时返回传输层 401/403；已建立工具调用中的业务错误按 MCP 工具错误语义返回统一对象，不能假装成功数据。

## 6. 医生交互、报告与运行状态

### 6.1 输入类型
| type | 行为 |
|---|---|
| QUESTION | 解释或追问，可指定 targetAgentId；不改变事实 |
| HYPOTHETICAL | 假设讨论，回答明显标记“假设”，不改变事实或正式报告 |
| FACT_SUPPLEMENT | 医生确认后的事实新增/更正，产生新快照；要求 confirmed=true |
| CONSULTATION | 请求允许范围内的会诊，产生新的分析运行 |

含糊输入默认 QUESTION；需要写入事实时先返回确认请求。targetAgentId 必须存在且当前有权使用；不允许自由文本指定任意工具或服务地址。

输入与执行路径固定如下，所有新运行仍受会话活动锁约束：
| 输入与当前条件 | 执行路径 | 允许产物 |
|---|---|---|
| QUESTION，尚未选定主诊 | 助理解答接诊/流程问题；无已核验病例时不进行个体辨证 | 回答消息，不创建正式报告 |
| QUESTION，已有主诊或上轮因缺项结束 | 原主诊的受控解释分支；已授权的显式目标优先，不能因此改变主诊分配 | 解释缺项理由和已有意见，不重跑整套四诊、不把旧 BLOCKED 步骤恢复 |
| HYPOTHETICAL | 同样进入解释分支，明确隔离假设与当前事实 | 标记假设的回答，不生成或修改正式报告 |
| FACT_SUPPLEMENT，confirmed=true | 在事务内创建新快照和 ANALYSIS 运行，重检专家范围及全部必要资料 | 新事实版本及新的分析结果；不续旧 run |
| CONSULTATION | targetAgentId 必填；创建 ANALYSIS 运行，重新核验主诊及所请求会诊的范围和准备阶段 | 新报告；会诊数仍至多1，未有主诊时先分流 |

- 解释分支使用 `runKind=EXPLANATION`，助理及专家技能必须另含经校验的 `explanationStep`（固定 stepId=explain、phase=EXPLAIN），按第4.12节声明输入输出、完成条件、询问范围、章节及 NONE/IF_NEEDED 只读 RAG 工具策略；沿用同一工具权限、步骤审计及状态约束，不执行 HIS 补查、不修改四诊步骤，也不绕开技能加载、风险、权限与预算检查。无已核验病例时只允许接诊流程或一般知识解释，不能用假设补齐病例事实。
- 正式分析使用 `runKind=ANALYSIS`；仅 analyses、FACT_SUPPLEMENT 或 CONSULTATION 创建此类运行。QUESTION 需要重新分析时提示医生显式发起，不暗中生成同快照新报告。资料尚未核验时解释运行不挂虚构 snapshotVersion。
- 医生确认属于业务 REST 流程，不采用 HumanInTheLoopHook 保持旧运行悬停；需要中断点恢复是后续独立设计，不能因接入框架 Hook 改变当前恢复承诺。

### 6.2 状态机
- 会话：OPEN、CLOSED；关闭后拒绝新分析和补充，仍可读取授权历史。
- 运行：QUEUED → RUNNING → SUCCEEDED / PARTIAL / NEEDS_INFORMATION / RISK_ESCALATED / OUT_OF_SCOPE / FAILED / CANCELLED。
- 报告：DRAFT → PENDING_REVIEW → APPROVED / REJECTED；任一已有报告可因快照或审核改版变为 STALE，保留原审核事件。
- 分析 SUCCEEDED 只代表技术运行成功，不代表临床确诊或医生审核通过。
- 同一会话同时只允许一个活动运行；并发请求使用会话行锁、版本号与幂等键控制。
- 医生补充与活动运行互斥，返回 409 提示先取消或等待；禁止旧运行完成后覆盖新快照结果。
- 新报告可以基于同一快照，但有独立 reportVersion；只有当前有效报告可审核。

### 6.3 报告结构
```json
{
  "reportId": "report-example",
  "reportVersion": 1,
  "snapshotVersion": 1,
  "analysisAsOf": "2026-09-21T09:00:00+08:00",
  "completion": "PARTIAL",
  "patientSummary": "基于已核实资料的摘要",
  "riskAlerts": [],
  "participants": [],
  "syndromeCandidates": [],
  "agreements": [],
  "disagreements": [],
  "missingInformation": [],
  "followUpQuestions": [],
  "evidence": [],
  "limitations": ["仅供医生辅助辨证"],
  "reviewStatus": "PENDING_REVIEW"
}
```
- 每个候选包含 `candidateId/name/school/supportingFactIds/opposingFactIds/evidenceIds/missingInformation`。
- 分别展示患者事实、文献、相似病例；引用 ID 必须指向实际工具结果或医生确认补充。
- 引用片段要匹配原文，模型不能生成不存在的 DOI、出处、页码或病例。
- 医生的诊疗确认单独记录，不把 APPROVED 自动解释为所有候选都已确诊。
- 修改审核时提交编辑后的报告及理由，生成新报告版本并审核新版本，不覆盖原报告。
- 不提供处方、剂量、医嘱字段；若模型生成越界内容，拦截并使用第6.4节全轮共享的一次纠正额度，不另开重试额度；仍失败则不发布结果。
- 只展示简要临床依据及证据，不展示模型隐藏思考过程。

### 6.4 oMLX 结构化输出适配
- 分流、步骤和汇总使用各自 DTO/JSON Schema，不以一个最终报告 schema 约束所有中间输出；统一适配器解析、校验并转换为业务对象。框架 call/invoke 返回文本还是 structured_output，由1.1.2.2验证后封装，领域层不依赖该细节。
- 默认 `CHAT_OUTPUT_MODE=PROMPT_JSON`：以 DTO 生成明确格式指令、应用解析 JSON 并执行 Schema、事实引用、权限和医疗边界校验；格式指令进入最终预算。该模式不主动请求服务端原生格式约束，不表示模型必然输出合法 JSON。
- 可选 NATIVE_SCHEMA / TOOL_SCHEMA 仅在 D2 证明对应 oMLX 模型支持后显式启用。使用 OpenAiChatModel 不代表 OpenAI 兼容服务支持相同 response_format；必须验证参数被接受且实际生效、与 tool_calls 共存，以及未知参数被忽略/拒绝的行为。
- 使用 outputType/outputSchema 前验证框架实际选择的原生、提示增强或工具策略。若框架自动选择与配置不同，使用适配器显式构造已验证请求；未能约束则该模式不可启用，不在运行中静默切换。
- TOOL_SCHEMA 的格式化工具属于应用内部本地工具，不发往 MCP；固定名称、schema和允许阶段，不允许借其调用 HIS/RAG，也不能用其成功替代 SOP 完成。额外 schema、模型请求和格式纠正统一计入预算。
- 无效 JSON、错误引用、越界内容及漏调必要工具，全 run 的所有 Agent 和阶段共用最多一次模型纠正请求，不按错误类型、步骤或阶段重置；已耗尽额度或总预算不足则不再请求。纠正也受当前步骤、权限及来源检查约束，仍不合格不发布结果。生产不记录完整失败正文，合成数据验证可在受限测试产物中检查实际请求。

## 7. 业务 REST API

### 7.1 公共约定
- 前缀 `/api/tcm`，JSON UTF-8；本系统资源 ID 使用 UUID 字符串，外部 patientId/visitId 保留 HIS 字符串格式，时间使用带时区 RFC3339。文中的 example ID 仅说明字段含义，不是可直接提交的 UUID。
- 普通成功响应 `{code:"OK",data:{},traceId:"..."}`；异步受理 HTTP 202，返回 runId/jobId。
- 错误响应 `{code,message,details,traceId}`；details 不含病历正文、密钥或 SQL。
- 变更接口必须带 `Idempotency-Key`，幂等范围为机构＋操作人＋路由＋键；同键不同请求体返回 409。
- 会话内变更的 `expectedVersion` 指 tcm_session.version，reportVersion 指报告业务版本，两者不可混用；RAG 版本管理接口的 expectedVersion 指对应可变版本行的锁版本。读取接口每次校验机构、操作者访问权。
- 默认分页 size=20、最大100；不可通过分页绕过机构权限。

### 7.2 接口清单
| 方法与路径 | 请求关键字段 | 结果/约束 |
|---|---|---|
| GET /experts | specialties?、size | 当前有权且启用的专家ID、AI名称、专长、症状范围和限制；不返回技能路径或内部提示词 |
| POST /sessions | patientId、visitId、pageSource、analysisAsOf?、chiefComplaint? | 201，sessionId、version；首次取数经助理 MCP 完成 |
| GET /sessions/{id} | 无 | 上下文摘要、当前快照/报告、运行状态，不回显身份证 |
| POST /sessions/{id}/analyses | expectedVersion、refreshHis=false、primaryAgentId? | 202，runId；未指定专家则由助理分流，指定后仍校验适用范围；有活动运行则409 |
| GET /sessions/{id}/runs/{runId} | 无 | runKind、阶段、routingStatus/匹配理由/候选与已选专家、各Agent当前stepId/phase/步骤状态/缺项、工具摘要、终态、responseMessageId与回答内容、reportVersion及错误；解释运行不产生reportVersion，不返回完整提示词 |
| POST /sessions/{id}/runs/{runId}/cancel | expectedVersion | 请求取消；晚到结果不落为有效报告 |
| POST /sessions/{id}/messages | type、content、targetAgentId?、confirmed?、expectedVersion | 202，messageId、runId；补充事实需显式确认 |
| GET /sessions/{id}/reports/{version} | 无 | 报告与审核历史、referenceStatuses及checkedAt；调用内部只读复核，不创建run，来源不可用/无权时按第9.3节限制展示 |
| POST /sessions/{id}/reviews | reportVersion、decision、reason?、editedReport?、expectedVersion | APPROVE / REJECT / AMEND；只处理当前有效版本 |
| POST /sessions/{id}/close | expectedVersion | 关闭会话；存在活动运行先返回409 |
| POST /sessions/{id}/case-submissions/preview | reportVersion | 脱敏预览、previewId、contentHash；不投递 |
| POST /sessions/{id}/case-submissions | previewId、contentHash、consentConfirmed、sourceAuthorization | 202，submissionId；预览及报告版本须仍有效 |
| GET /sessions/{id}/case-submissions/{submissionId} | 无 | 投递状态、RAG sourceId、知识库处理状态 |

刷新 HIS 没有新资料时复用当前快照，但产生新的运行；有新资料时产生新快照并使旧报告过期。专家分配在分析运行内完成，不另开绕过会话锁的直接转诊接口；未完成分流时 routingStatus 可为空，主诊未选定时 primaryAgentId 为空，consultantAgentIds 为空数组。分流理由及步骤摘要只面向有权访问当前会话的医生。

### 7.3 错误码
| HTTP | code | 场景 |
|---|---|---|
| 400 | VALIDATION_FAILED | 枚举、长度、格式或必要字段错误 |
| 401 / 403 | UNAUTHORIZED / ACCESS_DENIED | 未认证或无当前病例/资料权限 |
| 404 | RESOURCE_NOT_FOUND | 授权范围内找不到资源，不泄露其他机构是否存在 |
| 409 | VERSION_CONFLICT / RUN_ACTIVE / IDEMPOTENCY_CONFLICT | 版本、活动任务或幂等冲突 |
| 422 | ENCOUNTER_MISMATCH / MODEL_OUTPUT_INVALID / CONTEXT_BUDGET_EXCEEDED / BUDGET_EXCEEDED | 身份归属不符、结果无法校验、关键上下文无法容纳或调用次数耗尽；异步任务在终态返回错误码 |
| 429 | CAPACITY_EXCEEDED | 本地任务容量达到上限 |
| 503 / 504 | DEPENDENCY_UNAVAILABLE / DEPENDENCY_TIMEOUT | 数据、模型或 RAG 服务不可用/超时 |

已202受理的任务通过运行状态暴露最终失败，不用虚假的200报告覆盖错误。无法安全继续的上下文问题在异步运行中返回 NEEDS_INFORMATION 及 CONTEXT_BUDGET_EXCEEDED / CONTEXT_COMPACTION_FAILED / STALE_CONTEXT，不改变已返回的202，也不发布完整诊断报告。摘要失败但满足第4.8节降级条件时，压缩失败作为可查询告警保留，不伪造失败摘要或成功压缩状态。

### 7.4 记忆与上下文接口
以下接口仍使用 `/api/tcm` 前缀；不注册为 Agent 可写工具。机构、医生和主体范围来自当前认证及会话归属，不接收模型自报 scopeKey。
| 方法与路径 | 请求/用途 | 约束 |
|---|---|---|
| GET /sessions/{id}/messages | afterSeq、size | 分页读取有权消息；返回类型、有效性及来源，不能跨 Agent 私有可见范围 |
| GET /sessions/{id}/context | agentId | 授权上下文摘要、覆盖水位、快照及各分项 token、压缩状态；不返回完整系统提示词 |
| GET /sessions/{id}/memories | scope=ENCOUNTER/PATIENT、size | 读取有权历史及来源/状态/时间，默认排除失效项；不代表均用于当前报告 |
| POST /sessions/{id}/memories | scope、memoryKey、type、content、sourceRefs、时间字段、confirmed=true、expectedVersion | 显式确认事实/具体诊断/就诊摘要；校验会话和来源，创建 ACTIVE 版本及同步任务，201 |
| POST /memories/{id}/revisions | content、sourceRefs、reason、时间字段、confirmed=true、expectedVersion | 当前临床事实更正先走 FACT_SUPPLEMENT；这里仅更正记忆，事务内版本冲突返回409 |
| POST /memories/{id}/revocations | reason、expectedVersion | 立即停用并递增记忆修订与 epoch，202表示投影清理尚可在处理中 |
| GET /me/preferences | 无 | 仅当前医生的显示偏好 |
| PUT /me/preferences | displayDetail、reportLayout、expectedVersion | 显式白名单配置，不接收患者信息、任意系统指令或医学判断偏好 |

- 临床记忆 type 仅允许 CLINICAL_FACT、CONFIRMED_DIAGNOSIS、ENCOUNTER_SUMMARY；内容最多8000字符、sourceRefs最多20个、reason最多1000字符。临床来源必填且已核实，候选报告引用必须有医生对具体结论的明确确认。
- 创建时 expectedVersion 校验会话 version；记忆更正/撤销校验对应 memory record 的 version；偏好校验当前医生偏好记录 version（首次为0）。患者/就诊记忆写入限当前仍获授权的诊疗用途，关闭会话不接受新创建。
- `memoryKey` 是同 scope 内稳定的业务条目标识；同键创建冲突返回409，由客户端显式更正旧记录，不能依赖语义相似度覆盖。相同事实的重复与冲突同时纳入规则及人工核查。
- 同患者其他会话的记忆更正可使正在运行的 scope epoch 失效；不得绕过第6.2节直接修改该会话快照。记忆修改不等于 HIS 或当前病历修改。

### 7.5 记忆服务内部 REST 契约
前缀 `/internal/memory`，仅 tcm 服务身份可调用；不是面向前端或 MCP 的接口。适配服务必须验证短期签名服务上下文（audience=memory），绑定操作人、机构、允许的 scope/subjectKey、请求哈希、到期时间，不能仅信任请求体中的自报机构；令牌、密钥不落日志，生产传输使用 TLS。
| 接口 | 输入 | 返回 |
|---|---|---|
| POST /search | query≤2000字符、受签名约束的 scope、generation、topK=5（1～10） | 候选 memoryId/revision/generation/score；无匹配与不可用分开 |
| POST /projections | eventId、memoryId、revision、scope、generation、operation=UPSERT/REVOKE、contentHash、payloadHash、必要脱敏内容/来源元数据 | 202及receiptId；同代次同投递键且载荷一致返回原回执，异载荷409 |
| GET /receipts/{eventId} | 签名机构范围 | PENDING/RUNNING/SUCCEEDED/FAILED、外部映射、错误码；读取再次授权 |

tcm 注入记忆前必须回查自己的权威表，不能以适配服务的候选 ID、score 或成功回执替代医学确认。Mem0 API 参数、过滤语法及额外状态以锁定 OSS 版本验证，不直接混用托管 Platform 示例。

## 8. RAG 文献与病例入库

### 8.1 文献流程
```text
文件上传＋来源与授权信息
→ 类型/大小/哈希检查
→ 隔离解析
→ 正文规范化与片段定位
→ 脱敏和人工预览
→ 独立知识库审核
→ Embedding 生成与未发布代次写入
→ 完整性校验与原子发布
```
- 文件支持文本 PDF、DOC、DOCX，默认最大20 MB；禁止执行宏、外部引用和任意 URL 抓取。
- PDF 保留页码；Word 保留标题路径、段落编号，不生成不可靠的页码。
- 扫描或混合扫描内容未完整提取时标记 NEEDS_OCR/NEEDS_REVIEW；加密或损坏文件返回明确原因，不发布空文献。
- 解析放在独立受限工作进程，默认60秒超时，限制内存、解压体积和输出长度；超时终止该工作进程。仅取消 Java Future 不视为已隔离解析风险。
- 上传失败和临时文件由受控清理任务处理，原文件路径不可直接对外访问。
- 标题、资料类型、来源说明和使用授权必填；作者、发表日期、DOI未知时留空，禁止模型补造。
- 审核者标注证据类型与适用范围：GUIDELINE、TEXTBOOK、STUDY、EXPERIENCE、DEMO；这不是模型自动计算的证据等级。

### 8.2 切分与向量化
- 先按标题、段落及 PDF 页拆分，再按版本固定的 token 切分器处理；默认目标500 token、重叠80 token。
- 必须服从所选 Embedding 模型的实际输入长度。记录 tokenizer 和预处理版本；无法准确计算时用保守上限并验证，不依赖接口静默截断。
- 每片段保留原文跨度、sourceVersion、chunkOrdinal、contentHash和定位；相邻重复内容在检索汇总时去重。
- 入库和查询使用同一 Embedding 模型、维度、归一化及预处理。聊天模型与 Embedding 模型分别配置。
- 启动时以合成文本校验向量维度、有限数值和非零范数；不符合配置就阻止索引/查询就绪。
- cosine 距离检索，使用 HNSW；向量维度必须符合当前 pgvector 索引能力，禁止静默裁剪。
- 模型/维度/切分变化创建新 generation；维度变化使用对应的新向量表或 schema。表名由服务端注册，不接收模型生成名称。
- 每个检索集合记录 activeGenerationId；查询先固定该代次再使用对应 Embedding 配置，不能以新模型查询旧向量。模型迁移期间旧代次继续提供查询；新代次全量重建、追平发布变更并验证后，在短事务内切换集合和来源代次指针。旧模型不可用时返回 UNAVAILABLE，不混用模型继续查询。

### 8.3 病例入库
病例输入包含 `submissionId/sourceSystem/sourceAuthorization/synthetic/clinicalFeatures/confirmedDiagnoses/outcome`。
- clinicalFeatures 只保存脱敏临床内容；confirmedDiagnoses 必须是医生明确确认的结论，AI候选单独标记且默认不进入确诊字段。
- outcome 缺失时明确 UNKNOWN，不能把未随访病例描述为治疗成功。
- 检索向量主要基于当前可观察症状、四诊和相关病史，不把预期诊断标签作为唯一检索依据。
- 当前患者的历史排除使用服务端生成的机构内 HMAC subjectToken，只用于精确过滤、不进入模型和向量文本；它仍属于受保护的去标识化数据，不声称绝对匿名。
- 合成病例始终标记 synthetic=true；开发查询允许，真实临床 profile 必须排除。
- 相似病例是比较线索，不是文献证据等级的替代，不直接复制其处方。

### 8.4 收录审批
1. 医生审核当前有效报告。
2. 独立申请脱敏预览，医生确认临床字段、授权和预览哈希。
3. tcm 将 submission 与 Outbox 写入同一个 MySQL 事务。
4. 后台以稳定 submissionId 幂等调用 RAG REST；网络失败可安全重试。
5. RAG 接收病例，校验脱敏、授权及来源，进入独立审核。
6. 知识库审核通过后索引发布；诊疗审核不自动等于知识库发布。

同机构同 submissionId 同内容重试返回同一结果；不同内容返回409。预览后事实/报告变化，旧预览失效。未由医生主动提交前不得自动生成知识库记录；提交后的待审核记录尚不可检索。

### 8.5 资料与任务状态
- 资料版本：DRAFT → PROCESSING → PENDING_REVIEW → APPROVED → INDEXING → PUBLISHED。
- 分支：NEEDS_OCR、NEEDS_REVIEW、REJECTED、FAILED、WITHDRAWN；内容修正创建新版本，不修改已发布正文。
- 任务：PENDING、RUNNING、RETRY_WAIT、SUCCEEDED、FAILED、CANCELLED；记录 attempts、leaseOwner、leaseUntil、nextRunAt。
- 审核通过与索引任务在 PostgreSQL 同事务提交。
- 解析/Embedding 在事务外执行，向量以未发布 generation 分批写入；短事务复核版本、审核和撤回状态后切换 publishedGenerationId。
- 检索关联同库发布、机构权限及 generation；未完成向量即使已写入也不可检索。
- 撤回事务先禁止检索，再异步清理向量/缓存；晚到索引任务不能恢复撤回资料。
- 不使用 RAG 双数据源或 MySQL→pgvector 同步；RAG 普通表与向量共享 PostgreSQL 数据源及 JDBC 事务管理器。

## 9. RAG 检索与管理 API

### 9.1 检索策略
- 文献、病例使用独立向量集合；服务端绑定机构和权限，在 SQL 查询中加入发布版本、类型与授权过滤。
- 每类初始召回20项，文献去重后每个来源最多2片段，最终默认5条、最多10条。
- 一期不增加 reranker 或外部关键词引擎；无权资料不能为凑足 topK 被返回。
- 最邻近结果只称“候选来源”，不因 similarityScore 高就标记为支持证据。相关性门槛按实际模型验证后配置，不定义通用医学可信度分数。
- 无候选返回 NO_MATCH；有候选但不能支持当前问题，由报告标记证据不足。
- 检索支持和反向文献，保留病例的相似点与差异；不只召回支持初始假设的内容。
- read_reference 再次检查权限和发布版本；检索后被撤回的来源返回 STALE_SOURCE。
- 报告展示及提交前的系统来源复核经第9.3节内部 REST，不启动模型、不冒用历史 Agent 步骤；实际文献检索和原文读取仍经 MCP。依赖不可用时显示“当前来源状态未核验”，不能显示已确认仍有效。
- 历史报告保留当时授权证据快照作审计；撤回后不用于新分析，原内容按资料许可和留存政策限制访问。

### 9.2 RAG 管理接口
前缀 `/api/rag`；只向授权管理端和 tcm 提交工作流开放，不注册成 Agent 工具。
| 方法与路径 | 请求/用途 | 结果 |
|---|---|---|
| POST /documents | multipart：file、metadata(JSON) | 202，sourceId、version、jobId |
| POST /sources/{id}/versions | 新文献文件或修订病例；expectedVersion | 202，新版本及任务，不覆盖旧版 |
| POST /cases | 脱敏病例 JSON；Idempotency-Key | 202，sourceId、version；重复提交返回原结果 |
| GET /sources/{id}/versions/{version} | 读取预览、脱敏结果、索引状态 | 不直接暴露存储路径 |
| GET /sources/{id}/versions/{version}/file | 经授权下载原文 | 审计下载，不允许任意路径 |
| POST /sources/{id}/versions/{version}/reviews | decision=APPROVE/REJECT、reason、expectedVersion | 通过则创建索引任务 |
| POST /sources/{id}/versions/{version}/withdrawals | reason、expectedVersion | 立即禁止新检索，启动清理 |
| GET /case-submissions/{submissionId} | tcm 查询投递后的知识库状态 | sourceId、version、审核与索引状态 |
| GET /jobs/{id} | 查询任务 | 阶段、错误码、重试状态 |
| POST /jobs/{id}/retries | 受控人工重试 | 202，必须仍满足发布前置条件 |

RAG 的幂等、分页、错误结构与业务 API 一致。普通医生没有默认知识库发布权限，ROLE_KNOWLEDGE_REVIEWER 独立授权。

### 9.3 系统引用状态复核接口
选择独立内部 REST，保留 Agent MCP 签名不变，不新增系统 MCP 工具。
| 方法与路径 | 请求 | 返回 |
|---|---|---|
| POST /internal/rag/reference-statuses | requestId、reportId、reportVersion、references[{referenceId,version}]，每批1～100项 | 200，逐项 status=VALID/STALE_SOURCE/NOT_ACCESSIBLE、checkedAt、traceId；只返回请求中的引用，不返回原文或存储路径 |

- 仅接受 tcm 服务身份；短期签名上下文使用独立 audience=rag-reference-status，绑定当前操作者、机构、授权范围、reportId/reportVersion、请求哈希、exp/jti。tcm 先验证当前报告访问权并从其已保存证据构造引用集合；RAG 独立核验来源权限和版本，不信任客户端任意引用列表。
- 无权来源与不存在来源统一 NOT_ACCESSIBLE，不泄露当前版本或正文；已撤回/版本失效返回 STALE_SOURCE。传输及认证失败使用401/403/503/504，不假装 VALID；tcm 将未成功复核项标为 UNVERIFIED。
- 已关闭会话也可按权限读取历史报告；本接口不创建 run、不要求 active step、不复用旧 JWS。只读 POST 不要求变更幂等键，每次检查保留独立 requestId 及双方审计，重查允许结果变化。
- 每次报告展示的复核总期限5秒、每批最多3秒，分批合计不得超过总期限，不自动重试；超限未查项为 UNVERIFIED。提交前按相同上限并受该 run 剩余期限约束：待提交报告先由应用预分配 reportId/reportVersion，候选正文及证据保存为不可对外发布的 DRAFT；最终提交事务再次校验版本及引用集合哈希。复核发现 STALE_SOURCE/NOT_ACCESSIBLE 时终止为 NEEDS_INFORMATION/STALE_CONTEXT；UNVERIFIED 则按 DEPENDENCY_UNAVAILABLE/DEPENDENCY_TIMEOUT 失败，不把未通过复核的 DRAFT 发布为有效报告。
- 返回的 VALID 仅代表 checkedAt 时的状态，不是跨服务原子一致性承诺。正常展示隐藏 NOT_ACCESSIBLE 来源正文；失效/未核验来源不展示“当前有效”标记，历史审计原文仅按当前授权及资料留存许可另行访问，不用缓存绕过权限。

## 10. 数据模型与索引要求

### 10.1 公共设计约定
- UUID 主键；tcm MySQL 用 char(36)，RAG PostgreSQL 用 uuid。
- 每表按职责保存 organization_id、created_at、updated_at；可变业务行带 version 乐观锁。
- MySQL 时间使用 UTC datetime(6)，PostgreSQL 使用 timestamptz；对外保留时区。
- 枚举用字符串并由应用及迁移约束校验；大文本不可拼入 SQL，全部参数化。
- JSON 只保存聚合快照或不稳定结构，不替代需要筛选/唯一约束的关键字段。
- 不建跨服务外键；资源访问必须先验证机构和操作者，不凭 UUID 难猜作为权限措施。

### 10.2 tcm：MySQL＋JPA
| 表 | 核心字段 | 必要约束/索引 |
|---|---|---|
| tcm_session | patient_id、visit_id、page_source、operator_id、status、current_snapshot_version、current_report_version、active_run_id、next_message_seq、version | 索引 organization_id＋patient_id＋visit_id；会话行控制活动运行及消息序号分配 |
| tcm_snapshot | session_id、snapshot_version、analysis_as_of、facts_json、sources_json、completeness、content_hash | UNIQUE(session_id,snapshot_version)，不可变 |
| tcm_message | session_id、seq、agent_id、run_id、type、role、content、snapshot_version、visibility、source_refs、supersedes_message_id | UNIQUE(session_id,seq)；身份与正文分离；原消息不覆盖，来源有效性读取时复核 |
| tcm_run | session_id、snapshot_version、run_kind、status、stage、budget_json、config_versions_json、context_manifest_json、routing_json、error_code、lease_owner、lease_epoch、lease_until、version | 索引 status＋lease_until；manifest记录消息水位、scope epoch、摘要/记忆/来源/技能版本及分项token；routing记录分流依据及专家分配；lease_epoch用于拒绝旧执行者；EXPLANATION不产生报告，无已核验快照时snapshot_version为空，不虚构版本 |
| tcm_sop_step | run_id、agent_id、step_id、phase、skill_version、skill_hash、status、input_refs、output_refs、missing_items、skip_reason、error_code、version | UNIQUE(run_id,agent_id,step_id)；phase允许PREPARE/ANALYZE/EXPLAIN，按run_kind及技能契约校验；短事务推进，步骤记录不等于框架可恢复检查点 |
| tcm_agent_result | run_id、agent_id、result_json、completion、evidence_ids | UNIQUE(run_id,agent_id) |
| tcm_report | session_id、run_id、report_version、snapshot_version、body_json、status、version | UNIQUE(session_id,report_version) |
| tcm_review | report_id、decision、reviewer_id、reason、reviewed_at | 追加事件，不覆盖历史 |
| tcm_tool_audit | run_id、agent_id、step_id、skill_version、tool_kind、tool_name、call_id、attempt_no、request_hash、source_refs、duration_ms、outcome | UNIQUE(run_id,call_id,attempt_no)，关联实际步骤；tool_kind区分MCP/LOCAL_SKILL/LOCAL_FORMAT；不存令牌/完整身份证 |
| tcm_reference_check | request_id、operator_id、report_id、report_version、reference_set_hash、result_json、checked_at、outcome | request_id唯一；按report_id＋checked_at索引；系统复核独立审计，不伪造run或step |
| tcm_model_call_audit | request_id、run_id、agent_id、purpose、attempt_no、input_hash、token_count、duration_ms、outcome | request_id唯一；purpose区分TASK/SUMMARY/FORMAT/CORRECTION，失败调用也记账，不存完整提示词 |
| tcm_case_submission | session_id、report_version、preview_hash、payload_json、status、rag_source_id | submissionId 稳定；同版本相同内容防重复 |
| tcm_outbox | submission_id、payload_json、status、attempts、next_run_at、lease_until | UNIQUE(submission_id)，索引 status＋next_run_at |
| tcm_idempotency | organization_id、operator_id、route、key、request_hash、response_ref | 唯一键覆盖幂等范围 |

病例预览含 previewId、绑定报告版本、脱敏内容及过期时间，默认30分钟；预览可存 submission 的 PREVIEW 状态记录。正文属于敏感数据，按授权和留存策略访问。

### 10.3 RAG：PostgreSQL 普通表＋pgvector
| 表 | 核心字段 | 必要约束/索引 |
|---|---|---|
| rag_source | kind、title、owner_org、access_scope、synthetic、published_version_id、version | scope 只允许 OWNER_ORG / AUTHORIZED_SHARED |
| rag_source_version | source_id、version_no、status、metadata_jsonb、normalized_content、content_hash、file_key、published_generation_id | UNIQUE(source_id,version_no) |
| rag_case_profile | source_version_id、clinical_features_jsonb、confirmed_diagnoses_jsonb、outcome_jsonb、subject_token | source_version_id唯一；subject_token精确过滤 |
| rag_chunk | source_version_id、chunk_no、content、locator_jsonb、content_hash、splitter_version | UNIQUE(source_version_id,splitter_version,chunk_no) |
| rag_review | source_version_id、reviewer_id、decision、reason、authorization_ref | 追加审核历史 |
| rag_collection | kind、active_generation_id、version | 文献/病例各一条，查询固定活动代次 |
| rag_generation | collection_id、embedding_model、model_revision、dimensions、preprocess_version、vector_table、status | 向量空间定义不可变 |
| rag_job | source_version_id、generation_id、type、status、attempts、lease_owner、lease_until、next_run_at | 同版本/代次/任务类型幂等 |
| rag_embedding_* | chunk_id或case_version_id、generation_id、embedding vector(D)、filter_metadata | 每代次文献/病例独立表；唯一内容对象＋generation |
| rag_submission_receipt | caller_org、submission_id、request_hash、source_version_id | UNIQUE(caller_org,submission_id) |
| rag_audit_event | actor_id、action、resource_id、request_id、outcome、trace_id | 索引 resource_id＋created_at；REFERENCE_STATUS_CHECK通过request_id关联tcm系统复核 |

- pgvector HNSW 索引使用 cosine 运算符类；普通 B-tree 覆盖机构、资料版本、状态、代次和 subject_token。
- PgVectorStore 默认表若无法表达必要关联过滤，使用参数化 JDBC 实现检索；不得以库封装限制为由取消权限过滤。
- `rag_source_version` 与片段正文是权威内容；向量可重新生成。只有 RAG 服务可直接访问这些表。
- Flyway 管理扩展/表/索引；扩展安装由部署账号执行，运行账号仅有必要权限。

### 10.4 记忆与上下文新增表
以下 tcm 表均在业务 MySQL 使用 JPA，沿用机构隔离和乐观锁约定：
| 表 | 核心字段 | 必要约束/索引 |
|---|---|---|
| tcm_agent_context | session_id、agent_id、snapshot_version、current_summary_id、covered_through_seq、version | UNIQUE(session_id,agent_id)；摘要发布CAS，不保存不可恢复的唯一进程状态 |
| tcm_context_summary | context_id、summary_version、snapshot_version、scope_epochs_json、covered_through_seq、source_message_ids、source_refs、body_json、model_version、token_count、status | UNIQUE(context_id,summary_version)；正文不可变，READY/STALE/FAILED；引用失效不可用 |
| tcm_context_artifact | session_id、run_id、kind、source_id、source_version、locator、content_hash、authorized_payload、retrieved_at | 索引 session_id＋source_id＋source_version；存受控工具资料供回取，不存令牌/隐藏思考 |
| tcm_memory_scope | organization_id、scope、subject_key、epoch、version | UNIQUE(organization_id,scope,subject_key)；写入锁和跨会话变更检测 |
| tcm_memory_record | scope_id、memory_key、type、current_revision、status、version | UNIQUE(scope_id,memory_key)；PENDING_CONFIRMATION/ACTIVE/CONFLICT/REVOKED |
| tcm_memory_version | memory_id、revision、content、content_hash、occurred_at、recorded_at、valid_from、valid_to、confirmed_by、confirmed_at、synthetic | UNIQUE(memory_id,revision)；版本正文不可变，撤销同样递增revision |
| tcm_memory_source | memory_id、revision、source_type、source_id、source_version、fact_id、confirmation_ref | 索引 source_type＋source_id＋source_version；反向定位受影响记忆，源失效时停用并递增epoch |
| tcm_memory_sync_job | event_id、memory_id、revision、operation、scope_id、generation、payload_hash、status、attempts、lease_owner、lease_until、next_run_at、receipt_id | UNIQUE(memory_id,revision,operation,generation)及UNIQUE(organization_id,event_id)；同投递键稳定event_id，跨代次新建，索引 status＋next_run_at |
| tcm_memory_audit | memory_id、revision、actor_id、action、reason、trace_id、created_at | 追加事件，不覆盖历史 |

- DOCTOR 偏好使用 memory record/version 的独立 scope、type=DISPLAY_PREFERENCE 和固定 memoryKey=display；结构化白名单读取，一期不发送 Mem0。只有 ACTIVE 当前版本且来源有效的临床记忆可索引。
- 原始消息 visibility 至少区分 SESSION、TARGET_AGENT；私人讨论不因记忆检索而扩散，临床共享来自显式确认的记忆记录及其来源授权，不继承整段聊天可见性。
- 记忆服务独立 PostgreSQL 保存 projection_receipt、projection_mapping、memory_generation 及 Mem0 所需表：回执按机构＋eventId及第4.9节业务投递键分别唯一，保存payloadHash；映射绑定机构、memoryId、revision、generation与外部ID。另设 projection_barrier，以机构＋memoryId唯一保存最新revision及撤销状态，跨代次防止旧任务重新发布；不得访问 tcm MySQL。
- 索引可从 MySQL 权威版本经授权同步重建；记忆服务的内部历史状态和附加索引必须验证持久化/并发边界。未验证前只运行单个记忆 worker，不宣称 Mem0 已支持多实例写入。
- 原文、摘要、投影、同步载荷、备份分别配置留存和机构容量告警；读取分页，配额达到上限时显式拒绝新增，不静默删掉医疗记录腾空间。生产留存/配额由机构确认，不以模型窗口大小推导。

## 11. 异步、事务与恢复
- 网络调用、模型推理、Embedding、文件解析均不占用长数据库事务。
- tcm 的创建消息/提交任务与相关业务变更在本地事务内完成；工作线程领取后执行外部调用。
- DB任务用短事务加行锁领取，设置租约和尝试次数；租约过期只能在状态/版本复核后接管。
- RAG 向量批次可重试，依据确定性内容对象ID＋generation幂等写入；发布指针只在完整校验后的事务内切换。
- tcm→RAG 使用至少一次投递＋幂等接收，不承诺分布式 exactly-once。
- 诊疗运行崩溃后标记 FAILED/PROCESS_INTERRUPTED，显式重试创建新 run；不宣称能从模型内部思考中点恢复。租约失效时递增 lease_epoch，旧执行者的工具执行、步骤写回和报告提交必须同时匹配 lease_owner/lease_epoch、未过期租约及有效运行状态；不匹配则丢弃迟到产物。
- 报告提交前先在事务外完成受控来源复核，再在短事务内锁定所用 scope（按ID排序）、session、run，重新校验 epoch、会话快照、active_run_id、取消状态及租约后写报告和终态；检查与写入之间不释放锁。涉及这些行的其他复合事务遵循相同锁序，防止“检查通过后更正、旧报告仍提交”的窗口。
- 记忆变更在提交后仍通过 scope epoch 检查使旧报告/上下文不可作为当前有效分析；读取时检查当前权威状态。跨 RAG/HIS 不承诺分布式原子快照，保留来源 checkedAt 和无法获知上游变化的限制。
- 取消为协作式：尽力中断网络请求、禁止后续工具调用；迟到响应只记录取消审计，不成为有效报告。
- RAG 临时网络/限流错误最多自动重试3次，退避5秒、30秒、120秒；参数、权限、格式错误不自动重试。
- 诊疗模型超时默认不自动重试；无效结构或漏调必要工具使用第6.4节全轮共享的一次纠正额度，计入同一总预算，不另设纠正循环。
- Outbox最多自动投递5次，之后保留FAILED并允许人工重试；RAG幂等收据保留期不得短于发送端重试期。
- 记忆同步使用独立 tcm_memory_sync_job，不复用绑定 submissionId 的病例 Outbox。临时错误最多自动投递5次，退避5秒、30秒、120秒、300秒；永久参数/权限错误直接失败，人工重试前复核当前revision和撤销状态。
- 模型调用、摘要、记忆检索及索引不得占用长数据库事务；摘要落库和上下文指针切换在短事务内CAS，跨会话记忆事务锁定scope并递增epoch。实例切换只恢复已提交记录，不重复拼接历史。
- Mem0 关闭时保留待同步任务；重新启用先校验版本/代次、复核当前权威状态再追平。旧写任务不得跨过撤销事件；投影回执保留期不短于发送方可能重试期。

## 12. 配置、预算与可观测性

### 12.1 必需运行配置
| 配置 | 服务 | 要求 |
|---|---|---|
| OMLX_BASE_URL / CHAT_MODEL / CHAT_API_KEY | tcm | 地址、准确模型ID、按部署提供认证；不写入仓库 |
| EMBEDDING_BASE_URL / EMBEDDING_MODEL / EMBEDDING_DIMENSIONS / EMBEDDING_MODEL_REVISION | RAG | 与索引代次一致；不假设聊天模型支持向量化 |
| MYSQL_URL / MYSQL_USER / MYSQL_PASSWORD | tcm | 仅当前业务库 |
| POSTGRES_URL / POSTGRES_USER / POSTGRES_PASSWORD | RAG | 普通表和pgvector共用数据源 |
| HIS_MCP_URL / RAG_MCP_URL | tcm | 完整MCP端点，固定白名单，不从医生输入取地址 |
| MCP_CONTEXT_SIGNING_KEY / VERIFY_KEY | 对应服务 | 外置密钥，区分audience；不记录原始值 |
| RAG_FILE_ROOT | RAG | 专用目录，禁止越界访问 |
| DEV_AUTH_CONFIG | 三个Java服务及记忆适配服务 | 外置测试令牌到固定机构/角色映射，仅dev profile |
| MEMORY_SERVICE_URL / MEMORY_CONTEXT_SIGNING_KEY / MEMORY_CONTEXT_VERIFY_KEY | tcm / 记忆服务 | 内部REST白名单与独立audience签名；密钥外置 |
| MEMORY_POSTGRES_URL / MEMORY_POSTGRES_USER / MEMORY_POSTGRES_PASSWORD | 记忆服务 | 独立记忆数据库，不使用RAG账号 |
| MEMORY_EMBEDDING_BASE_URL / MEMORY_EMBEDDING_MODEL / MEMORY_EMBEDDING_DIMENSIONS / MEMORY_EMBEDDING_REVISION | 记忆服务 | 本地模型配置和记忆代次一致，不默认回退云服务 |
| CHAT_CONTEXT_WINDOW / CHAT_OUTPUT_RESERVE / CHAT_TOKENIZER_REVISION | tcm | 实际窗口、输出预留、tokenizer/chat template版本；缺失或不兼容不得启用推理 |
| MEMORY_SDK_VERSION / MEMORY_GENERATION | 记忆服务 | D2验证后锁定SDK及当前索引代次，变更需兼容/迁移测试 |
| RAG_INTERNAL_BASE_URL / RAG_STATUS_SIGNING_KEY / RAG_STATUS_VERIFY_KEY | tcm / RAG | 内部状态复核地址及独立audience签名，不作为模型工具 |
| CHAT_OUTPUT_MODE | tcm | 默认PROMPT_JSON；NATIVE_SCHEMA/TOOL_SCHEMA须匹配已验证的模型能力配置 |
| SKILL_RESOURCE_BASE / SKILL_STAGING_ROOT | tcm | 仅获准打包资源及专用解包目录；禁止默认用户技能目录与运行中热更新 |

模型服务地址约定为服务根地址，由适配层追加 `/v1/chat/completions` 或 `/v1/embeddings`，启动校验避免重复 `/v1`。本地地址不是云模型地址，不自动回退云端。

### 12.2 开发默认预算
| 参数 | 默认值 | 超限行为 |
|---|---|---|
| 每会话活动运行 | 1 | 409 RUN_ACTIVE |
| 单推理执行实例的模型并发 / 等待运行 | 1 / 20 | 新任务429，不无限排队；不是多实例集群总并发保证 |
| 单Agent模型调用 / 全轮模型调用 | 6 / 20 | 按实际模型请求计数，跨阶段不重置；超限终止并报告预算不足 |
| 单步骤工具尝试 / 全轮工具尝试 / 单响应工具数 | 6 / 40 / 4 | 本地工具、MCP及重试均计入；超限拒绝执行并显式终止 |
| 单次工具 / 单次模型 / 整轮运行 | 15秒 / 120秒 / 300秒 | 记录超时，不能发布完整结果 |
| RAG Embedding并发 / 解析工作进程 | 1 / 1 | 数据库排队 |
| 文献上传 / 解析时间 | 20 MB / 60秒 | 拒绝或任务失败 |
| 每次检索topK / 最大值 | 5 / 10 | 非法参数400 |
| MCP执行上下文有效期 | 60秒 | 拒绝过期上下文 |

整轮预算从开始执行计时，排队最长60秒。模型与上下文token上限必须按实际模型配置；预算不足时明确内容未覆盖，不悄悄丢弃风险资料。
- 一期同一模型端点只部署一个启用推理 worker 的 tcm 实例；其他实例可提供业务读取。两个实例恢复测试通过顺序切换 worker 验证，不能将各进程并发1宣传为集群并发1；多活推理前另行实现跨实例许可并压测。
- read_skill/read_skill_section 本地执行不额外算模型请求，但提出该调用的模型请求计数，本地执行耗时和工具次数也计入。摘要、路由、格式化、纠正及框架重试逐次记账，不能用图节点数或一个 agent.call 次数代替实际模型请求数。
- 每次网络调用、重试及退避使用 min(单项上限, 剩余总期限)；没有剩余预算不开始调用。模型/工具次数在执行前预占，失败不退还；同一模型请求不要因多层拦截重复计数。预算耗尽使用 NEEDS_INFORMATION/BUDGET_EXCEEDED，依赖超时按相应失败终态处理。
- D2 交付逐次调用账本，覆盖助理、主诊/会诊两阶段、汇总、技能读取、分页、摘要和纠正；6/20次、工具6/40/4及300秒是待实测默认值，不能为通过测试静默放宽。

### 12.3 观测
- 记录 traceId、runId、agentId、工具名、来源版本、耗时、状态、调用次数和token用量（服务返回时）。
- 不默认记录完整提示词、患者正文、身份证、访问令牌、向量或模型内部思考。
- 业务审计与调试日志分开；病例内容只存受控业务记录。
- 健康检查区分 liveness 和 readiness；RAG readiness校验PostgreSQL、扩展、向量空间配置，Embedding依赖状态单独暴露。
- 阶段状态与错误可查询；没有运行本地模型时不得声称已经完成真实推理或检索联调。
- 上下文指标：各分项token、压缩前后token/耗时、触发原因、重复摘要次数、原文回取次数、关键事实保留校验、工具消息完整性、scope epoch冲突及记忆降级次数；不以压缩率替代医学质量。
- 本地token计数与模型返回usage对照校准；记录误差、模板和模型版本，误差超出安全余量时停止相关配置的推理就绪，不能靠静默截断兜底。

### 12.4 上下文容量、阈值与配置
持久化历史容量与模型一次读取容量分开：数据库按配额/留存分页保存，模型仅获取有限工作集。多个Agent窗口不相加；服务并发、KV Cache及长上下文延迟需独立压测，不能由窗口大小推算生产吞吐。
```text
W = min(模型支持窗口, 推理服务配置窗口, 实测可接受窗口)
B = W - O - S
C = 下一次完整请求的输入token
U = C / B
O：输出预留（按模型计费/限制语义包含占用输出预算的推理token）
S：安全余量；B必须大于0
C：包含系统规则、当前输入、技能、工具schema、患者事实、消息、摘要、记忆、证据及chat template开销
```
| 条件 | 动作 |
|---|---|
| U < 0.70 | 正常运行，仍做去重、来源有效性及保护项检查 |
| 0.70 ≤ U < 0.85 | 调用前软压缩一次已完成历史，避免频繁全量总结 |
| U ≥ 0.85 | 暂停新披露，强制整理；无法安全降到85%以下则返回受限终态 |
| C > B | 硬拦截，绝不发送超限请求；等于B仍需通过85%规则 |
| 压缩目标 C ≤ floor(0.55 × B) | 目标非强制，不以移除关键资料达标；55%与70%间留缓冲 |

预测即将返回的资料可能越线时先缩减分页/披露范围；实际返回后再计数。阈值比较使用精确比例，不因展示四舍五入漏掉边界；不是按固定第N轮触发，也不是框架自动配置。

以下是待由应用绑定的配置示例，32768/4096/2048仅演示32K有效窗口，不表示当前部署能力。实际窗口必须由部署输入和模型验证确定，不把示例兜底成默认值。
```yaml
tcm:
  context:
    window-tokens: 32768
    output-reserve-tokens: 4096
    safety-margin-tokens: 2048
    compact-trigger-ratio: 0.70
    compact-urgent-ratio: 0.85
    compact-target-ratio: 0.55
    recent-turns-max: 6
    recent-tokens-max: 4096
    summary-tokens-max: 2048
    semantic-memory-tokens-max: 2048
    compact-attempts-per-check: 1
  memory:
    semantic-enabled: false
    search-top-k: 5
    search-timeout-ms: 3000
```
- 配置校验：`0 < target < trigger < urgent < 1`，各token上限为正，输出预留不得超过模型实际输出能力，window不得高于验证值；子预算是最大值，不要求填满，最终受B约束。
- 示例B=26624；软阈值约18637、强制阈值约22631、目标最多14643 token。中文不能使用“字符数÷4”作为可靠计数；计数器须匹配tokenizer和chat template，不可获取时使用经过样例验证的保守上界，未验证不启用真实推理。
- 最近6轮同时受4096 token限制，按完整交互单元选择；关键事实另行保护。摘要无法安全压入2048时分段保存、选择当前相关段，仍超限则标记未覆盖；不得硬切正文。
- 摘要输出限额按summary-tokens-max与模型输出能力取小值；每个分段请求占用第12.2节6/20次及300秒总预算，超限不另开无限预算。Mem0查询最多3秒，失败降级，所有时间均计入本轮预算。
- soft/urgent/target为开发基线，需以长病例、长对话、工具膨胀和关键事实保留评测调参；调整记录配置版本，不将其宣传为已验证医学阈值。

## 13. 安全与临床边界
- dev profile仅绑定本地回环地址，固定测试机构/医生/知识审核角色，使用外置测试认证配置；不接真实患者资料。
- 所有REST、MCP操作校验权限；生产身份由医院SSO/授权体系接入后替换dev认证，不能把自报operatorId作为身份。
- 仅传递最小必要临床信息到本地模型；本地推理不自动等于满足数据合规要求。
- RAG跨机构共享须有明确授权；知识库发布与诊疗审核分权，Agent没有发布或撤回权限。
- 人工审核脱敏结果，自动过滤不能保证匿名；患者身份、罕见组合与正文残留标识均纳入检查。
- 文献/病历提示词注入不能改变工具权限、数据范围或系统流程；服务端独立校验输入和输出。
- 未经医学审核的规则、学派配置与示例只能用于开发演示。
- 输出供医生辅助辨证，不自动执行诊疗；风险场景交由当前诊疗团队处理，免责声明不能代替实际拦截与审核。
- 真实数据的合法来源、患者信息处理依据、留存与备份、删除策略、文件访问审计及医疗合规评估是上线前条件。
- 摘要、长期记忆、Embedding及同步载荷同属敏感衍生数据；记忆范围过滤、按ID读取/更正/撤销和来源回取均需服务端授权。关闭或失效后不能由缓存、旧摘要或旧向量重新注入。
- 生产容量与保留期须依据机构确认和压测；一期单并发只是开发配置，不代表多用户上线容量。

## 14. 开发任务与交付顺序
| 阶段 | 工作包 | 交付物与完成条件 |
|---|---|---|
| D1 基础工程 | 三个Java服务及记忆适配服务结构、配置、迁移、认证边界、依赖核验 | 各自启动；tcm只连MySQL，RAG和Mem0各用独立PostgreSQL库 |
| D2 模型与协议 | 固定1.1.2.2框架接入、oMLX Chat/Embedding/结构化输出、MCP、模拟HIS、Mem0兼容性验证 | 完成下列D2最小验证清单，记录实际请求与依赖版本；未通过不启用对应能力，不以官网示例替代验证 |
| D3 RAG入库 | PDF/Word解析、片段、病例接口、审核、异步索引 | 已审核资料可检索；未发布资料不可见 |
| D4 RAG查询 | 三个只读MCP工具、内部引用状态接口、权限过滤、原文定位、撤回 | 检索与原文经MCP，系统复核经受签名REST；引用可追溯，故障与无匹配区分 |
| D5 Agent编排 | 外层StateGraph、分流、独立SKILL、分阶段SOP、解释分支、专家隔离及上下文压缩 | 主诉→全部专家准备→冻结→主诊/会诊辨证→汇总→医生审核；步骤证据、最终请求守卫、撤权及70%/85%边界通过 |
| D6 业务闭环 | 医生交互、审核、病例Outbox、长期记忆确认/更正/撤销、独立同步任务 | 先完成MySQL权威记录与失效治理，再接入D2验证通过的Mem0；假设不入事实、旧投影不复活 |
| D7 联调验收 | 实际oMLX/Mem0、长对话、跨实例恢复、并发更正、租约失效、故障降级、容量及医学对照评测 | 列明通过项、阻塞项与未验证边界；单/双专家、记忆开关及压缩策略有可比结果，不把合成测试当临床效果 |

D2 最小验证清单（仅用合成病例）：
1. 锁定1.1.2.2及实际传递依赖，运行可执行 JAR，验证受控技能发现、完整主文件读取、章节白名单、版本哈希与无用户目录覆盖。
2. 验证技能工具激活→进入NONE步骤→最终请求无MCP工具→伪造调用被拒；覆盖空tools默认行为、旧groupedTools残留、压缩后重载和阶段重入。
3. 验证接诊→双专家PREPARE→共享快照冻结→分别ANALYZE→汇总；会诊独有必要资料在冻结前获取，REQUIRED与有据复用均有真实记录。
4. 验证每次模型/工具循环及动态schema后的最终守卫；含格式化、摘要、纠正和退避的调用账本与剩余期限一致，不重复计数、不漏计、不双重执行工具。
5. 验证专家输入只含获准交接与自己的历史，父消息、私人讨论和隐藏思考不传播；解释分支可回答缺项理由，不恢复旧BLOCKED步骤。
6. 验证oMLX结构化输出模式、参数生效及结果提取；注入额外格式化工具时仍受白名单约束，无效结果不能成为报告。
7. 验证报告提交/展示的内部引用复核，无活动run及已关闭会话也能授权读取；独立audience、逐项状态、限时降级和审计正确。
8. 保留tokenizer/窗口、Embedding维度及Mem0版本/过滤/更正/重复投递验证；未运行真实模型的测试与真实联调结果分开标识。

本文件只定义开发要求，不表示已经创建上述模块、数据库或接口。

## 15. 测试与验收标准
| 编号 | 对应需求 | 可验证结果 |
|---|---|---|
| AC-01 | FR-01 | 正确患者/就诊读取成功，错误归属拒绝，不能返回默认患者 |
| AC-02 | FR-01/03 | MCP工具由模型提出、Java执行；未获取病历时不输出已读病历的报告 |
| AC-03 | FR-02 | 主诊/会诊只来自注册专家，至多1名会诊，无覆盖范围时明确提示 |
| AC-04 | FR-04 | QUESTION/HYPOTHETICAL不更新快照；确认补充产生新快照并使旧报告失效 |
| AC-05 | FR-04 | 风险场景停止常规辨证推进，输出交由医生处理的提示，无处方/剂量/医嘱 |
| AC-06 | FR-05/06 | tcm仅需要MySQL；RAG在无MySQL配置情况下完成入库、审核、检索 |
| AC-07 | FR-03/06 | 文献和相似病例均经RAG MCP，引用ID/版本/定位与实际返回一致 |
| AC-08 | FR-07 | PDF/Word解析可定位；扫描/加密/损坏文件不能静默成为可检索文献 |
| AC-09 | FR-06/07 | 未审核、未完成索引、撤回及其他机构无权资料均不可检索 |
| AC-10 | FR-07 | 报告批准不自动收录；预览确认→Outbox→RAG独立审核→发布完整可追溯 |
| AC-11 | FR-06/08 | Embedding维度不符拒绝就绪；换模型需新代次，不混合向量空间 |
| AC-12 | 全部 | 超时、预算、重试、取消、并发补充和服务崩溃均有明确终态，无串患者 |
| AC-13 | 全部 | 模型/文献注入不能跨权限调用工具，日志不包含原始身份令牌或完整病历 |
| AC-14 | FR-06 | 分批索引失败可重试；发布与撤回并发后已撤回来源不能恢复可见 |
| AC-15 | FR-08 | 实际oMLX完成聊天→工具调用→工具结果→结构化回答，Embedding完成真实检索 |
| AC-16 | FR-09 | 长对话经过多次压缩仍保留关键事实、否认、冲突、时间及假设标签；原始消息可授权回取 |
| AC-17 | FR-09 | 两个tcm实例交替处理已提交会话，消息不重复、Agent不串历史；中断run不被当成自动续跑成功 |
| AC-18 | FR-10 | 同患者跨会话/就诊只取得获准历史，同医生换患者不串用；不同医生私人讨论不泄露，跨机构拒绝 |
| AC-19 | FR-10 | 报告批准、普通回答和摘要压缩均不自动批准长期事实；显式确认后才可索引 |
| AC-20 | FR-10 | 两会话并发更正发生版本冲突；旧向量未清理、旧同步晚到、回执丢失重试均不能恢复失效记忆 |
| AC-21 | FR-10 | Mem0关闭/超时使用结构化降级且明确提示；MySQL或授权失败不绕过；同revision跨代次重建不复用旧回执，重建期间更正/撤销不复活，换代次不混用向量 |
| AC-22 | FR-11 | 覆盖70%/85%/100%边界及上下一个token；最终请求含工具schema/模板仍不超B，中文计数经模型校验 |
| AC-23 | FR-11 | Skills重复加载、超长工具返回、分页和专家交接均触发守卫；内容移出窗口后需要时重新读取 |
| AC-24 | FR-09/11 | 摘要CAS拒绝旧水位/快照结果；压缩后工具调用与结果成组；摘要失败仅在第4.8节全部降级条件满足时继续并告警，否则或保护项超预算时明确终止 |
| AC-25 | FR-09/10/11 | 容量未到阈值时更正/撤回也使上下文失效；调用前及报告提交前能阻止新旧版本混用 |
| AC-26 | FR-11 | 单模型并发下摘要不死锁；多段摘要计入6/20次和300秒预算，无递归压缩或无限重试 |
| AC-27 | FR-02/12 | 主诉症状匹配专家有事实/规则依据，否认、假设、历史及未核验主诉不误作当前阳性事实；不按专家名称或单一词命中直接转交 |
| AC-28 | FR-02/12 | 多主诉、分流缺项、无合适专家、风险、人群/场景排除及医生指定均按规则处理；无兜底乱分配或无限转诊 |
| AC-29 | FR-03 | 两名专家读取各自独立SKILL并体现不同四诊关注点；共享公共资料不替代独立SOP，缺失技能/非法路径/未知工具阻止启用 |
| AC-30 | FR-03/12 | NONE/IF_NEEDED/REQUIRED步骤分别验证无MCP工具、条件调用/有据复用、必须实际调用；本地技能/格式化工具单独受控；跨步骤或跨Agent越权被拒，不能仅凭模型自报完成 |
| AC-31 | FR-03/04/12 | 望闻问切已有资料不重复问诊，缺项不虚构；医生确认补充后新快照/新run重检步骤；冻结后不继续注入新患者事实，无自动处方/医嘱 |
| AC-32 | FR-03/09/11/12 | 分流及SOP状态可查询，工具调用关联步骤和技能版本；循环调用与压缩均过守卫，运行中不热换SOP且中断不冒充自动恢复 |
| AC-33 | FR-03/12 | 可执行JAR中读取每专家主文件和章节，未知sectionId/越界/用户同名覆盖被拒；未加载必需内容不得执行步骤 |
| AC-34 | FR-03/11/12 | NONE最终请求无MCP schema，技能激活后步骤撤权生效；空tools、默认工具、动态工具合并不能恢复旧权限，伪造调用不出网 |
| AC-35 | FR-09/11 | Hook之后新增提示/工具/输出schema仍经最终守卫；实际请求与预算一致，超限/取消/失效不发送，摘要与纠正无漏计 |
| AC-36 | FR-02/03 | 两专家全部PREPARE先于冻结；会诊独有取数步骤可执行，冻结后禁止补查；阶段重入不重置预算、不重复追加历史 |
| AC-37 | FR-04/09 | QUESTION/HYPOTHETICAL使用独立explanationStep及EXPLAIN阶段，只产回答且不续旧run；缺失契约或混入分析步骤被拒，无病例不伪造快照；确认补充和会诊走新分析，私人讨论不跨专家传播 |
| AC-38 | FR-06/12 | 无活动run及关闭会话可按授权复核历史报告，系统REST与Agent MCP身份隔离；无权/不存在不泄露，撤回及超时不显示有效 |
| AC-39 | FR-08/11 | 各启用输出模式在实际oMLX通过验证，参数忽略/不支持被识别；额外格式化工具及请求受预算约束，多Agent/多阶段错误共用一次纠正额度，非法JSON/引用不发布 |
| AC-40 | FR-09/10 | 更正在最终检查与提交之间发生时旧报告不能提交为当前有效；租约失效/取消的迟到步骤和报告被拒，双实例恢复不突破单worker部署约束 |
| AC-41 | FR-02/03/11 | 固定医学评测集完成单/双专家、记忆开关、压缩策略对照；报告事实/风险错误、医生修改量与延迟，不把流程成功等同临床有效 |

测试层次：
- 单元测试：状态转换、输入验证、工具白名单、来源校验、文档定位和幂等逻辑。
- 数据库集成测试：Testcontainers MySQL验证JPA/Outbox；PostgreSQL＋pgvector验证普通表、索引与事务，不使用H2替代。
- 协议测试：真实MCP客户端/服务端往返、签名/过期上下文、分页、业务错误。
- Agent编排自动化：模拟模型响应稳定覆盖分支，不能替代实际oMLX联调。
- 固定合成病例覆盖门诊、住院时间线、资料缺失、冲突、多问题、急危重信号及越权场景。
- 分流/SOP用例增加主诉同义表达、否定句、适用范围外病例、多主诉、两专家不同流程、重复取数、步骤越权、工具失败及医生补充；医学人员定义预期专家或允许的候选集合与拒绝条件，不将合成症状映射宣传为实际治疗能力。
- 医学对照评测使用固定病例、相同模型/知识库/预算及可追溯配置，按患者或病例来源隔离开发集与评测集，防止相似病例泄漏答案。比较单专家＋相同RAG、主诊＋会诊、记忆开关和压缩策略；相同病例重复运行，记录事实错误、否认/时间误读、风险遗漏、无依据结论、无效追问、医生修改量、审阅耗时及P95延迟。医学人员在测试前确定评分规则、拒绝条件和可接受阈值；未明确或未达标仅交付工程验证，不宣称临床可用。
- RAG评估准备至少20个带期望来源的查询，记录recall@5、来源定位准确率和无相关资料表现；初期作为基线，不冒充临床有效性指标。
- 权限、未发布可见性和引用真实性测试必须全部通过；医疗内容仍需独立医学审核。
- 记忆/压缩单元测试用确定性token计数器及模拟摘要覆盖边界、摘要不忠实拒绝、同输入不重复压缩；真实模型测试校准中文、chat template和工具schema计数。
- 集成测试同时运行两个tcm实例、独立RAG/记忆库和可故障注入的Mem0适配服务，覆盖签名范围、过滤、事务回滚、乱序任务、版本更正及撤销。
- 合成多轮病例包含上千轮历史、跨就诊事件和更正/假设/否认；比较压缩前后关键事实覆盖与错误引入，记录延迟和回取率。长历史仅分批读取，不要求一次输入模型，也不宣称任意长度无损摘要。

## 16. 联调前需提供的环境信息
以下是部署输入或真实接入门槛，不影响先按合成数据和接口契约开发：
- oMLX服务地址、准确聊天模型ID、工具调用模板支持情况及上下文上限。
- 本地Embedding模型ID、版本、维度、最大输入长度和接口地址。
- MySQL、PostgreSQL/pgvector连接及初始化权限；测试环境需可运行容器。
- 可合法使用的文献样例、合成病例样例、资料管理员和医学审核职责。
- 每位专家经审核的症状适用/排除规则、目标人群和门诊/住院范围、专属四诊SOP与对应SKILL.md、允许工具及缺项处理；仅角色名称不足以启用专家。
- 生产阶段另行提供HIS接口、pageSource映射、SSO/授权契约、数据合规与留存要求。
- Chat模型实际窗口、最大输出、tokenizer/chat template版本及长上下文测试结果；不能将第12.4节32K示例直接当部署能力。
- Mem0 OSS锁定版本、内部服务地址、记忆库凭据/初始化权限、附加状态存储方式和本地Embedding配置；语义记忆开关默认关闭，验证通过后显式开启。
- 上线前确认机构并发目标、记忆访问规则、数据配额和各类原文/摘要/投影/备份留存期，并单独完成容量与安全验证。

## 17. 参考与实施注意
- Spring AI Alibaba快速开始：https://java2ai.com/docs/quick-start
- Agents：https://java2ai.com/docs/frameworks/agent-framework/tutorials/agents
- Skills：https://java2ai.com/docs/frameworks/agent-framework/tutorials/skills
- Hooks和Interceptors：https://java2ai.com/docs/frameworks/agent-framework/tutorials/hooks
- Tools与MCP接入：https://java2ai.com/docs/frameworks/agent-framework/tutorials/tools
- 多智能体编排：https://java2ai.com/docs/frameworks/agent-framework/advanced/multi-agent
- 结构化输出：https://java2ai.com/docs/frameworks/agent-framework/tutorials/structured-output
- Spring AI MCP：https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
- oMLX接口与模型能力：https://github.com/jundot/omlx
- pgvector：https://github.com/pgvector/pgvector
- Spring AI Alibaba短期记忆：https://java2ai.com/docs/frameworks/agent-framework/tutorials/memory
- Spring AI Alibaba长期记忆：https://java2ai.com/docs/frameworks/agent-framework/advanced/memory
- Mem0工作机制：https://docs.mem0.ai/core-concepts/how-it-works
- Mem0 OSS版本迁移：https://docs.mem0.ai/migration/oss-v2-to-v3
- Mem0 OSS过滤：https://docs.mem0.ai/open-source/features/metadata-filtering

官方文档及其链接的 main 分支代码可能随版本更新；示例中默认消息共享、全量技能读取、工具激活、摘要、Shell或自动重试不能直接作为本项目配置。Spring AI Alibaba 统一采用1.1.2.2；D2以该版本对应源码、实际依赖和oMLX请求验证接入行为，不因官网其他推荐版本降级，不把本文设计要求写成已验证能力。
