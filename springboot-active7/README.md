# Active7 员工请假审批实验室

这是一个可以直接运行的 Activiti 7 教学项目。员工提交请假后，流程根据请假类型和天数，自动经过经理、HR、总监等节点，最后由员工确认并结束。

项目重点不是“画一张流程图”，而是把一套完整业务串起来：

- Spring Security 双 Token 登录、RBAC 权限和细粒度接口授权。
- Activiti 7 流程部署、流程实例、用户任务、流程变量和历史记录。
- 不同账号只能看到自己的候选任务或已签收任务。
- 条件网关、拒绝、退回补充、重新提交、撤回、员工最终确认。
- 业务表与 Activiti 流程表为什么要分开。
- MyBatis-Plus 实体映射、条件构造器、原子更新，以及通过关联字段和 XML JOIN 查询 RBAC 关系。
- JDK 8 开发者熟悉的 Java 写法，同时使用 JDK 21 运行 Spring Boot 3。

> 重要：这里的“JDK 8 风格”是指不用 `record`、`var`、switch 表达式、文本块等新语法，方便有 JDK 8 经验的开发者阅读；并不是说项目可以在 JDK 8 上运行。Spring Boot 3 和 Activiti 7 需要更高的 JDK，本项目使用 JDK 21。

---

## 文档导读

如果你是第一次接触 Activiti，建议按下面的顺序学习，不要一开始就钻引擎源码。

| 阶段 | 目标 | 对应章节 |
|---|---|---|
| 第 1 天：跑起来 | 准备 JDK、Maven、MySQL，启动项目并登录 | 1～8 |
| 第 2 天：走流程 | 用不同账号完成一次请假，观察任务流转 | 9～11 |
| 第 3 天：看数据 | 理解业务表、引擎表、MyBatis-Plus 和流程变量 | 12～14 |
| 第 4 天：看代码 | 从 Controller 读到 Service、Listener 和 Mapper | 15～18 |
| 进阶：调试与改造 | 使用 curl、SQL 排障，修改流程并做生产化评估 | 19～24 |

读完后，应该能独立回答三个问题：

1. 流程现在停在哪个节点？
2. 这个节点属于哪个账号？
3. 下一个节点由哪个流程变量决定？

---

## 1. 项目简介

### 1.1 业务场景

张三想在部门请假，于是提交申请：

1. 经理王五审批，可以同意、拒绝或退回补充。
2. 系统根据类型和天数判断是否需要 HR。
3. 请假达到 5 天时，还需要总监审批。
4. 所有审批通过后，由申请人确认请假安排。
5. 流程结束，最终通过的请假会写入审批轨迹并扣减年假。

李四也是一名员工，但他不会看到张三的确认任务，也不会看到不属于自己的经理任务。

### 1.2 项目边界

这是一个教学项目，不是一套可以直接上生产的 OA 系统。它刻意保留了容易观察和修改的部分：

- 审批人账号直接写在 BPMN 中，没有接入真实部门组织关系。
- 使用演示账号和固定初始密码。
- 页面使用 Thymeleaf 和原生 JavaScript，没有引入复杂前端框架。
- 只保存“是否提供医疗证明”，没有实现附件上传。
- 没有实现消息通知、催办、超时、代理审批和节假日计算。

这些简化可以让新人先理解 Activiti 的核心机制。生产化清单见第 23 章。

### 1.3 项目位置

本说明对应仓库中的 `springboot-active7` 模块：

```text
springboot-active7
├── pom.xml
├── README.md
└── src/main
    ├── java/com/example/active7
    │   ├── controller        HTTP 接口和页面路由
    │   ├── service           业务编排和 Activiti 操作
    │   ├── service/dto       请求表单和页面视图对象
    │   ├── mapper            MyBatis-Plus Mapper
    │   ├── model             数据库实体
    │   ├── security          Spring Security 用户加载
    │   ├── config            安全配置
    │   ├── init              演示数据初始化
    │   ├── workflow          流程常量与结束监听器
    │   └── common            API 异常处理
    └── resources
        ├── application.yml
        ├── application-mysql.yml
        ├── db/schema-mysql.sql
        ├── processes/leave-approval.bpmn20.xml
        ├── templates
        └── static
```

---

## 2. 技术栈与版本

| 技术 | 用途 | 本项目版本或说明 |
|---|---|---|
| Java | 运行和编译 | 使用 JDK 21，源码语法保持 JDK 8 风格 |
| Maven | 构建和多模块依赖管理 | Maven 3.9 或兼容版本 |
| Spring Boot | 应用框架、自动配置、事务 | 由父 POM 统一管理 |
| Spring Security | 双 Token 认证、RBAC、接口权限 | Spring Boot 3 配套版本，无状态 Bearer Token |
| JJWT | 创建和验证 Access Token | `0.12.5` |
| Activiti | BPMN 流程引擎 | `7.1.0.M6` |
| MyBatis | Activiti 与 MyBatis-Plus 共用的持久层基础 | `3.5.19` |
| MyBatis-Plus | 业务数据访问 | `3.5.7` |
| MySQL | 持久化业务数据和流程数据 | MySQL 8 |
| Thymeleaf | 服务端 HTML 模板 | Spring Boot 配套版本 |
| Knife4j/Swagger | 接口文档页面 | 来自公共 Web 模块 |

依赖版本主要在仓库根目录的 [pom.xml](../pom.xml) 中管理；模块自己的 [pom.xml](pom.xml) 只声明需要的依赖。

Activiti 使用 `7.1.0.M6`，因为 `M4` 自带的 MySQL 建表脚本没有部署表新增的 `VERSION_` 和 `PROJECT_RELEASE_VERSION_` 列，但 Mapper 已经使用这两列，首次自动部署会失败。针对 Spring Boot 3，模块还显式引入 `activiti-spring-identity` 和 `activiti-spring-resource-finder`，并在 [AutoConfiguration.imports](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) 中注册 M6 所需的身份、资源、项目模型和安全策略自动配置。MyBatis 显式使用 `3.5.19`，覆盖 Activiti 传递的旧版本，避免与 MyBatis-Plus `3.5.7` 不兼容。

### 2.1 为什么代码可以用 JDK 8 的语法阅读

模块 POM 中设置了：

```xml
<maven.compiler.source>8</maven.compiler.source>
<maven.compiler.target>8</maven.compiler.target>
```

源码刻意采用：

- 普通 Java 类，不用 `record`。
- 显式声明变量类型，不用 `var`。
- 普通 `if/else`，不用 switch 表达式。
- 显式 getter/setter，不用 Lombok。
- 普通字符串拼接，不用文本块。
- 显式集合泛型，例如 `new HashMap<String, Object>()`。

仍然使用 JDK 8 已经支持的 Lambda、Stream、`LocalDateTime` 和 `BigDecimal`。

### 2.2 为什么不能真的用 JDK 8 启动

`source/target = 8` 只表示“按这个语法级别编译”，不会把 Spring Boot 3 和 Activiti 7 变成 JDK 8 兼容库。运行环境仍然必须满足依赖要求，本项目统一使用 JDK 21。

---

## 3. 先理解 Activiti 的核心概念

### 3.1 流程定义、流程实例和任务

| 概念 | 可以类比成什么 | 在项目中的例子 |
|---|---|---|
| Process Definition | 一张流程图纸 | `leaveApproval` 流程定义 |
| Process Instance | 按图纸办理的一次具体业务 | 张三提交的一条请假 |
| Task | 流程停在某个节点等待人办理 | 王五的“部门经理审批”任务 |
| Variable | 办理过程中需要记住的数据 | `needHr`、`leaveDays`、`applicant` |
| History | 已经发生过的流程记录 | 张三这次请假的全部历史任务 |

一条流程定义可以启动很多流程实例。每启动一次请假，就产生一个新的流程实例。

### 3.2 常用 Service

| Service | 主要职责 | 本项目使用位置 |
|---|---|---|
| `RepositoryService` | 部署、查询流程定义和部署资源 | 自动部署、管理员接口、动态部署 |
| `RuntimeService` | 启动流程、读写流程实例变量 | `LeaveService.submit()`、完成任务前更新变量 |
| `TaskService` | 查询、签收、评论、完成任务 | 待办列表、签收、同意、退回、撤回 |
| `HistoryService` | 查询历史流程和任务 | 本项目主要通过引擎历史表观察 |

可以把它们理解为：

- `RepositoryService`：管理图纸。
- `RuntimeService`：让图纸跑起来，并维护运行时变量。
- `TaskService`：处理需要人参与的节点。

### 3.3 候选人和办理人

BPMN 中经理节点这样写：

```xml
<userTask id="managerApproval" name="部门经理审批"
          activiti:candidateUsers="wangwu"/>
```

员工确认节点这样写：

```xml
<userTask id="employeeConfirm" name="员工确认请假安排"
          activiti:assignee="${applicant}"/>
```

区别是：

- `candidateUsers`：候选账号。任务刚创建时办理人可能为空，候选账号可以先签收。
- `assignee`：直接指定办理人。`applicant` 来自流程启动变量，因此不同申请会分配给不同员工。

签收后，任务办理人从空变成当前账号，其他候选账号不能继续代替办理。

### 3.4 流程变量与条件网关

Java 在经理同意后设置：

```java
variables.put("needHr", needsHr(request));
variables.put("needDirector", request.getLeaveDays().doubleValue() >= 5.0D);
```

BPMN 网关读取这些变量：

```xml
<conditionExpression xsi:type="tFormalExpression">
    <![CDATA[${needHr == true}]]>
</conditionExpression>
```

变量名和类型必须对应。Java 中叫 `needHr`，XML 中也必须叫 `needHr`；Java 放 `Boolean`，XML 用 `true` 判断。

---

## 4. 系统架构与一次请求的链路

### 4.1 分层结构

```text
浏览器
  │  Access Token（请求头）＋ Refresh Token（HttpOnly Cookie）
  ▼
Spring Security Filter
  │  验证 JWT、重新加载用户角色与权限、执行接口授权
  ▼
Controller
  │  接收参数、调用业务、包装 ResultBody
  ▼
LeaveService
  ├── MyBatis-Plus Mapper ──► 业务表
  └── Activiti Service ─────► ACT_* 流程表
  │
  ▼
Thymeleaf 页面 / JSON 接口
```

Spring Security 和 Activiti 解决的是两个不同问题：

- Spring Security：账号是否有效、是否具有调用接口所需的权限编码。
- Activiti：这个待办任务是不是当前账号可以处理的。

两层检查缺一不可。

### 4.2 提交申请的链路

```text
POST /api/leave/requests
  → LeaveController.submit()
  → LeaveService.validateForm()
  → 从 SecurityContext 获取当前用户名
  → 插入 leave_request
  → 组装流程变量
  → RuntimeService.startProcessInstanceByKey()
  → 回写 process_instance_id
  → 插入 SUBMIT 轨迹
  → 流程停在 managerApproval
  → 返回 requestId
```

`LeaveService` 类上有 `@Transactional`。本地单库环境下，业务表写入和流程实例启动放在同一个 Spring 事务中；任一步抛异常，事务会回滚。

### 4.3 完成任务的链路

```text
POST /api/leave/tasks/{taskId}/approve
  → visibleTask() 再次校验任务归属
  → 校验审批动作和意见
  → 设置 approveManager / hrApproved 等变量
  → completeTask()
       → 必要时签收
       → 写业务轨迹和 Activiti 评论
       → 更新任务局部变量
       → 更新流程实例变量
       → complete()
       → 查询下一个活跃任务
       → 更新业务状态和 current_node
  → 如果流程结束，LeaveEndListener 更新终态和余额
```

---

## 5. 运行前准备

需要准备：

1. JDK 21。
2. Maven 3.9 或兼容版本。
3. MySQL 8，并已启动服务。
4. 可选的数据库客户端，例如 MySQL Client、IDEA Database 或 DBeaver。

检查 Java 和 Maven：

```bash
java -version
mvn -version
```

如果 `java -version` 不是 JDK 21，可以在当前终端临时指定。macOS 本机示例：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

Windows 开发者可以设置系统环境变量 `JAVA_HOME`，再把 `%JAVA_HOME%\bin` 加入 `Path`。

检查 MySQL 是否可连接：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p
```

输入实际密码后，如果进入 MySQL 命令行，说明客户端、端口和账号可用。

---

## 6. 初始化 MySQL 数据库

### 6.1 创建数据库

应用不会自动创建数据库本身，只会在已有数据库中创建业务表和流程表。先执行：

```sql
CREATE DATABASE springboot_active7
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 6.2 配置连接信息

打开 [application-mysql.yml](src/main/resources/application-mysql.yml)，把 `username` 和 `password` 改成本机 MySQL 的实际账号和密码。

项目配置约定：

- 配置文件中直接填写实际值，不使用 `${...}` 占位符。
- 不使用环境变量插值或配置属性引用。
- 敏感凭据不得提交到 Git；需要保密时使用未纳入版本控制的本地配置。
- 文档和聊天中不要复制真实数据库密码。

数据库名也要与创建语句一致，默认是 `springboot_active7`。

`application-mysql.yml` 还保存本机 JWT 签名密钥 `active7.security.jwt.secret`。密钥应使用足够长的随机字符串，不得提交到 Git，也不要复制到文档或聊天中。生产环境使用 HTTPS 时，把 `refresh-cookie-secure` 改为 `true`；本机 HTTP 学习环境保持 `false`。

### 6.3 谁负责建表

业务表由 [schema-mysql.sql](src/main/resources/db/schema-mysql.sql) 创建，配置如下：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema-mysql.sql
```

脚本使用 `CREATE TABLE IF NOT EXISTS`，每次启动都会执行，但不会清空已有数据。

Activiti 的 `ACT_*` 表由引擎根据下面的配置自动创建或更新：

```yaml
spring:
  activiti:
    database-schema-update: true
    check-process-definitions: true
```

因此，首次启动前只需要手动创建数据库，不需要提前手工创建所有 Activiti 表。

### 6.4 启动顺序总结

```text
启动 MySQL
  → 创建 springboot_active7 数据库
  → 填写 application-mysql.yml 的连接信息
  → 启动 Spring Boot
  → SQL 脚本创建 9 张业务表
  → Activiti 检查并创建流程表
  → Activiti 自动部署 processes 下的 BPMN
  → DemoDataInitializer 初始化角色、权限、关系、账号和年假余额
  → 应用接受请求
```

---

## 7. 启动项目

### 7.1 方式一：在 IDEA 中运行

1. 用 IDEA 打开仓库根目录。
2. 等待 Maven 导入依赖。
3. 将 Project SDK 设置为 JDK 21。
4. 找到 [Active7Application.java](src/main/java/com/example/active7/Active7Application.java)。
5. 右键选择 Run。
6. 打开 <http://localhost:8890/active/login>。

### 7.2 方式二：在仓库根目录用 Maven 运行

```bash
cd /Users/zhaojh/IdeaProjects/jdk21_projects/maven_projects/springboot-collection-jdk21

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

mvn -pl springboot-active7 -am spring-boot:run
```

`-pl springboot-active7` 表示只运行当前模块，`-am` 表示同时构建它依赖的公共 Web 和公共工具模块。

### 7.3 方式三：打包后运行

```bash
mvn -pl springboot-active7 -am clean package

java -jar springboot-active7/target/active7.jar
```

最终 JAR 名称由模块 POM 中的 `<finalName>active7</finalName>` 决定。

### 7.4 修改端口

```bash
java -jar springboot-active7/target/active7.jar --server.port=8891
```

此时访问：

```text
http://localhost:8891/active/login
```

本文后续示例仍使用默认端口 8890。

### 7.5 启动成功的标志

日志中应出现类似内容：

```text
Tomcat started on port 8890
Started Active7Application
```

默认上下文路径是 `/active`，来自 [application.yml](src/main/resources/application.yml)：

```yaml
server:
  port: 8890
  servlet:
    context-path: /active
```

所以页面地址是 `/active/login`，接口地址是 `/active/api/...`。

### 7.6 MySQL 启动失败时先看什么

常见错误：

```text
Communications link failure
Connection refused
```

优先确认：

1. MySQL 服务是否已经启动。
2. 3306 端口是否可连接。
3. `application-mysql.yml` 中的地址、账号和密码是否正确。
4. 数据库是否已经创建。
5. 启动日志是否激活了 `mysql` profile。

---

## 8. 演示账号与权限矩阵

| 账号 | 密码 | 姓名 | 角色 | 流程职责 |
|---|---|---|---|---|
| `zhangsan` | `123456` | 张三 | `EMPLOYEE` | 提交、补充、确认自己的请假 |
| `lisi` | `123456` | 李四 | `EMPLOYEE` | 验证员工之间的数据隔离 |
| `wangwu` | `123456` | 王五 | `MANAGER` | 经理审批、拒绝、退回补充 |
| `zhaoliu` | `123456` | 赵六 | `HR` | HR 审批 |
| `sunqi` | `123456` | 孙七 | `DIRECTOR` | 总监审批 |
| `admin` | `admin123` | 管理员 | `ADMIN` | 查看部署、定义、XML 和全部运行时任务 |

账号由 [DemoDataInitializer.java](src/main/java/com/example/active7/init/DemoDataInitializer.java) 初始化，密码通过 BCrypt 编码后保存。

### 8.1 RBAC 默认权限

| 角色 | 默认权限范围 |
|---|---|
| `EMPLOYEE` | 提交、查看、撤回自己的请假，查看余额，办理自己的补充和确认任务 |
| `MANAGER` | 员工权限，以及查看、签收、办理经理任务 |
| `HR` | 员工权限，以及查看、签收、办理 HR 任务 |
| `DIRECTOR` | 员工权限，以及查看、签收、办理总监任务 |
| `ADMIN` | 查看当前身份、全部请假、流程管理、动态部署、RBAC 查看和管理 |

角色只是权限的集合，接口最终检查的是 `leave:create`、`process:view`、`rbac:manage` 等权限编码。用户可以被分配多个角色，权限取所有角色权限的并集。

注意：

- 管理员不会自动成为请假审批人。
- 管理员可以在流程管理页查看全部运行时任务，但办理任务仍要经过 Activiti 的候选/办理人检查。
- 具有 `leave:view:all` 的账号可以查看全部业务申请；普通员工只能查看自己的申请。
- 演示账号已经存在时，初始化代码不会覆盖已有密码和资料。
- 默认角色权限和用户角色只在用户表为空的首次启动时播种；管理员在 RBAC 页面删除关系后，重启不会自动补回。
- 演示环境使用固定弱密码，仅供本机学习，不能用于生产。

---

## 9. 页面操作导览

登录后进入工作台，左侧区域根据当前权限显示，最多六个区域。

### 9.1 流程总览

显示：

- 我的年假余额。
- 当前待办数量。
- 我的申请数量。
- 当前登录角色和权限驱动的菜单。
- 请假流程简图。
- 最近申请。

“新建请假”按钮只在具有 `leave:create` 权限时显示。

### 9.2 我的申请

默认显示当前账号提交的申请，包括审批中、已通过、已拒绝和已撤回记录。具有 `leave:view:all` 权限时，标题切换为“全部申请”，并加载全部业务申请。

可以打开详情、查看审批轨迹，并在流程状态允许时撤回。

### 9.3 我的待办

只显示满足下面条件的任务：

```java
.taskCandidateOrAssigned(当前登录用户名)
```

候选任务需要先签收，已指定给当前账号的任务可以直接办理。

### 9.4 流程学习

提供三种 Service、任务分配、条件路由、数据模型和练习提示，适合对照源码阅读。

### 9.5 流程管理

只有具有 `process:view` 权限时才能看到该区域，包含：

- 流程定义。
- 部署记录。
- 上传 BPMN ZIP/BAR 动态部署。
- 全部当前运行时任务。
- 查看指定流程定义的 BPMN XML。

前端隐藏菜单只是界面优化，真正的权限拦截在 Spring Security 中。

### 9.6 权限管理

只有具有 `rbac:view` 权限时才能看到该区域：

- 左侧列出用户及已分配角色。
- 右侧列出角色及已配置权限。
- 选择用户后可以更换角色，选择角色后可以更换权限。
- 保存后立即重新读取 `/api/auth/me`，页面按最新权限显示。
- 服务端在每个请求中重新加载角色和权限，因此修改在下一次请求生效。

具有 `rbac:manage` 权限才能提交修改。系统会阻止移除最后一个管理员，也会阻止管理员角色丢失流程和 RBAC 管理必需权限。

---

## 10. 第一次完整演练：1 天年假

建议完全按顺序操作，这是理解 Activiti 最快的一条路径。

### 第 1 步：张三提交

1. 使用 `zhangsan / 123456` 登录。
2. 点击“新建请假”。
3. 选择“年假”，填写 1 天和请假事由。
4. 提交申请。

预期：

- 申请状态为 `SUBMITTED`。
- 当前节点为 `managerApproval`。
- 张三的待办仍为 0，因为经理任务不属于他。

### 第 2 步：王五查看经理任务

1. 退出张三。
2. 使用 `wangwu / 123456` 登录。
3. 进入“我的待办”。

预期：

- 看到“部门经理审批”。
- 办理人为空，处于待签收状态。

### 第 3 步：王五签收并同意

1. 点击“签收任务”。
2. 任务状态变为已签收。
3. 点击“同意”。
4. 填写意见，例如“同意，注意工作交接”。
5. 确认同意。

1 天年假不需要 HR 和总监，因此直接进入“员工确认安排”。

### 第 4 步：张三确认

1. 退出王五。
2. 重新使用 `zhangsan / 123456` 登录。
3. 进入“我的待办”。
4. 点击“确认安排”。
5. 填写确认意见并提交。

### 第 5 步：核对结果

预期：

- 申请状态为 `APPROVED`。
- 当前节点为 `END`。
- 张三年假余额从 10 天变成 9 天。
- 没有剩余待办。
- 轨迹包含提交、经理签收、经理同意、员工确认和流程结束。

这条路径只有两个用户任务：经理审批和员工确认。HR、总监节点被条件网关跳过。

---

## 11. 业务流程与分支规则

流程文件：[leave-approval.bpmn20.xml](src/main/resources/processes/leave-approval.bpmn20.xml)。

流程 key 为 `leaveApproval`，共 5 个用户任务、4 个排他网关。

### 11.1 节点一览

| 节点 ID | 节点名称 | 办理人 | 说明 |
|---|---|---|---|
| `managerApproval` | 部门经理审批 | `wangwu` | 同意、拒绝、退回 |
| `managerDecision` | 经理审批结果 | 自动 | 选择同意、退回或拒绝 |
| `hrCheck` | 是否需要 HR | 自动 | 读取 `needHr` |
| `hrApproval` | HR 审批 | `zhaoliu` | 同意或拒绝 |
| `directorCheck` | 是否需要总监 | 自动 | 读取 `needDirector` |
| `directorApproval` | 总监审批 | `sunqi` | 同意或拒绝 |
| `employeeResubmit` | 员工补充并重新提交 | 申请人 | 修改内容后回到经理节点 |
| `employeeConfirm` | 员工确认请假安排 | 申请人 | 最终确认后结束 |
| `leaveEnd` | 请假流程结束 | 自动监听器 | 更新终态、扣减年假、记录结束事件 |

### 11.2 自动分支

```text
所有申请
  └─ 部门经理审批
       ├─ 拒绝 → 流程结束，状态 REJECTED
       ├─ 退回 → 员工补充 → 重新计算条件 → 部门经理审批
       └─ 同意
            ├─ 不需要 HR → 总监条件判断
            └─ 需要 HR → HR 审批
                 ├─ 拒绝 → 流程结束，状态 REJECTED
                 └─ 同意 → 总监条件判断
                      ├─ 不需要总监 → 员工确认
                      └─ 需要总监 → 总监审批
                           ├─ 拒绝 → 流程结束，状态 REJECTED
                           └─ 同意 → 员工确认
                                └─ 员工确认 → 流程结束，状态 APPROVED
```

HR 条件由 `LeaveService.needsHr()` 计算：

- 请假天数大于 3 天；或者
- 类型为病假、婚假、丧假。

总监条件：

- 请假天数大于或等于 5 天。

示例：

| 请假内容 | 用户节点 |
|---|---|
| 1 天年假、1 天事假 | 经理 → 员工确认 |
| 0.5 天病假 | 经理 → HR → 员工确认 |
| 4 天事假 | 经理 → HR → 员工确认 |
| 5 天年假 | 经理 → HR → 总监 → 员工确认 |
| 10 天婚假 | 经理 → HR → 总监 → 员工确认 |

### 11.3 表单校验

请假类型：

```text
ANNUAL       年假
SICK         病假
PERSONAL     事假
MARRIAGE     婚假
BEREAVEMENT  丧假
```

校验规则：

- 天数为 0.5～30 天。
- 天数必须是 0.5 的整数倍。
- 结束时间不能早于开始时间。
- 病假超过 1 天必须提供医疗证明。
- 年假不能超过当前剩余余额。
- 拒绝和退回必须填写审批意见。
- 只有经理节点允许退回补充。

注解校验在 [LeaveRequestForm.java](src/main/java/com/example/active7/service/dto/LeaveRequestForm.java)，跨字段和数据库相关校验在 `LeaveService.validateForm()`。

### 11.4 撤回规则

员工可以在以下状态撤回：

- 经理尚未签收的 `managerApproval` 任务。
- 已退回的 `employeeResubmit` 任务。

不能撤回：

- 经理已经签收的任务。
- 已进入 HR、总监或员工确认的任务。
- 已经结束的流程。

### 11.5 年假扣减

只有最终状态为 `APPROVED` 且类型为 `ANNUAL` 才扣减：

- 拒绝：不扣减。
- 撤回：不扣减。
- 审批中：暂不扣减，避免流程回滚后无法恢复。
- 员工最终确认：进入结束监听器后扣减。

教学版按“当前自然年份”查询和扣减余额，生产系统应按请假所属年度、入职时间和公司年假规则重新设计。

---

## 12. 数据库设计：业务表与引擎表

### 12.1 业务表

脚本：[schema-mysql.sql](src/main/resources/db/schema-mysql.sql)。每张表和每个字段都添加了中文 `COMMENT`。

| 表 | 作用 |
|---|---|
| `sys_user` | 登录账号、姓名、部门、BCrypt 密码 |
| `sys_role` | 角色定义 |
| `sys_user_role` | 用户和角色关系，通过 `user_id`、`role_code` 关联 |
| `sys_permission` | 权限编码、名称、类型和资源范围 |
| `sys_role_permission` | 角色和权限关系，通过 `role_code`、`permission_code` 关联 |
| `sys_refresh_token` | Refresh Token 的 SHA-256 哈希、用户、过期和撤销时间 |
| `leave_request` | 请假申请当前状态和业务内容 |
| `leave_record` | 面向业务的统一审批轨迹 |
| `leave_balance` | 每人每年的年假额度与已用天数 |

业务表由应用代码维护，字段变化应通过受控脚本或数据库迁移管理。

如果数据库已经创建过旧版 `leave_record`，需要执行下面一次，补齐审批轨迹稳定排序字段；首次使用完整脚本建表时不需要重复执行：

```sql
ALTER TABLE leave_record
    ADD COLUMN record_seq BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '轨迹顺序号，保证同一秒内的记录稳定排序' AFTER id,
    ADD UNIQUE KEY uk_leave_record_seq (record_seq);
```

### 12.2 RBAC 关系模型

```text
sys_user
  └─ sys_user_role.user_id
       └─ sys_role.role_code
            └─ sys_role_permission.role_code
                 └─ sys_permission.permission_code
```

用户与角色、角色与权限都是多对多关系。项目不使用 MyBatis-Plus 的关联实体注解，也不在实体中嵌套其他实体，而是保留关系字段，在 XML Mapper 中通过 `JOIN` 查询。

### 12.3 Activiti 自动表

| 表 | 作用 |
|---|---|
| `ACT_RE_DEPLOYMENT` | 流程部署记录 |
| `ACT_RE_PROCDEF` | 流程定义和版本 |
| `ACT_RU_TASK` | 当前活跃的用户任务 |
| `ACT_RU_EXECUTION` | 当前流程执行位置 |
| `ACT_RU_VARIABLE` | 运行时流程变量 |
| `ACT_HI_TASKINST` | 历史任务记录 |
| `ACT_HI_PROCINST` | 历史流程实例 |
| `ACT_HI_COMMENT` | Activiti 任务评论 |

这些表由流程引擎维护，不要把它们当作普通业务表随意增删改。

### 12.4 两套记录的区别

`leave_record` 和 `ACT_HI_COMMENT` 看起来相似，但职责不同：

- `leave_record`：业务轨迹，覆盖提交、签收、同意、退回、确认、结束，便于业务查询和页面展示。
- `ACT_HI_COMMENT`：Activiti 引擎评论，偏向流程引擎能力。
- `ACT_HI_TASKINST`：引擎历史任务，记录任务何时开始、何时结束、由谁办理。

三者可以互相印证，但不能完全替代。

### 12.5 为什么需要业务键

启动流程时：

```java
runtimeService.startProcessInstanceByKey(
        LeaveProcessConstants.PROCESS_KEY,
        request.getId(),
        variables);
```

第二个参数 `request.getId()` 是业务键，把 Activiti 流程实例和 `leave_request.id` 关联起来。

流程实例 ID 生成后，又会回写到 `leave_request.process_instance_id`。结束监听器则通过 `execution.getProcessInstanceBusinessKey()` 找到业务申请。

### 12.6 表名大小写

不同操作系统和 MySQL 配置下，`ACT_*` 表可能显示为小写。SQL 示例使用小写形式；如果 Linux 上提示表不存在，先执行 `SHOW TABLES`，按实际表名调整大小写。

---

## 13. MyBatis-Plus 持久层详解

项目不再直接使用 `JdbcTemplate` 或 `RowMapper`。虽然依赖树中仍可能看到 `spring-boot-starter-jdbc`，那是 MyBatis-Plus Starter 的正常传递依赖；模块 POM 没有直接声明它。

### 13.1 实体映射

[LeaveRequest.java](src/main/java/com/example/active7/model/LeaveRequest.java) 中：

```java
@TableName("leave_request")
public class LeaveRequest {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String leaveType;

    @TableField("has_medical_proof")
    private boolean hasMedicalProof;
}
```

含义：

- `@TableName`：实体对应 `leave_request` 表。
- `@TableId`：指定主键列。
- `IdType.INPUT`：使用业务代码自己生成的 UUID，不依赖数据库自增或 MyBatis-Plus 雪花 ID。
- `@TableField`：显式指定不规则的列名。
- 开启下划线转驼峰后，`leave_type` 自动对应 `leaveType`，`create_time` 自动对应 `createTime`。

配置位于 [application.yml](src/main/resources/application.yml)：

```yaml
spring:
  mybatis-plus:
    configuration:
      map-underscore-to-camel-case: true
```

### 13.2 BaseMapper

[LeaveRequestMapper.java](src/main/java/com/example/active7/mapper/LeaveRequestMapper.java) 只需要：

```java
@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {
}
```

即可使用：

- `insert(entity)`。
- `selectById(id)`。
- `selectList(wrapper)`。
- `selectCount(wrapper)`。
- `update(entity, wrapper)`。
- `updateById(entity)`。

不需要为每张表手写 XML CRUD。

### 13.3 条件查询

```java
leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
        .eq(LeaveRequest::getApplicant, username)
        .orderByDesc(LeaveRequest::getCreatedAt));
```

`LeaveRequest::getApplicant` 是方法引用，MyBatis-Plus 会解析成数据库列 `applicant`。相比手写字符串列名，Lambda 形式更不容易在字段改名后留下错误 SQL。

### 13.4 部分字段更新

回写流程实例 ID：

```java
leaveRequestMapper.update(null, new LambdaUpdateWrapper<LeaveRequest>()
        .eq(LeaveRequest::getId, request.getId())
        .set(LeaveRequest::getProcessInstanceId, processInstanceId));
```

这种方式只更新指定列，不会把实体中其他未加载字段覆盖成空值。

### 13.5 原子扣减余额

[LeaveBalanceMapper.java](src/main/java/com/example/active7/mapper/LeaveBalanceMapper.java) 中：

```java
@Update("UPDATE leave_balance SET used_days = used_days + #{days} "
        + "WHERE user_id = #{userId} AND balance_year = #{year} "
        + "AND used_days + #{days} <= annual_days")
int addUsedDays(@Param("userId") String userId, @Param("year") int year,
                @Param("days") BigDecimal days);
```

不要先查询余额，再在 Java 中计算后无条件更新。并发情况下，两个请求可能同时读到同一个旧余额。

把“扣减”和“余额足够”放在同一条 SQL 中，由数据库保证原子性。返回更新行数为 0，就说明余额不足或记录不存在。

### 13.6 RBAC 联表查询

[SysUserRoleMapper.java](src/main/java/com/example/active7/mapper/SysUserRoleMapper.java) 只继承 `BaseMapper<SysUserRole>`，不在 Mapper 注解中写联表 SQL。

角色和权限查询集中在 [SysAuthorityMapper.java](src/main/java/com/example/active7/mapper/SysAuthorityMapper.java) 与 [SysAuthorityMapper.xml](src/main/resources/mapper/SysAuthorityMapper.xml)：

```xml
<select id="selectRoleCodes" resultType="string">
    SELECT r.role_code
    FROM sys_user_role ur
    JOIN sys_role r ON r.role_code = ur.role_code
    WHERE ur.user_id = #{userId}
    ORDER BY r.role_code
</select>

<select id="selectPermissionCodes" resultType="string">
    SELECT DISTINCT p.permission_code
    FROM sys_user_role ur
    JOIN sys_role_permission rp ON rp.role_code = ur.role_code
    JOIN sys_permission p ON p.permission_code = rp.permission_code
    WHERE ur.user_id = #{userId}
    ORDER BY p.permission_code
</select>
```

查询结果直接映射成字符串列表，不建立嵌套 `resultMap`。RBAC 页面所需的“用户及角色”“角色及权限”也使用 `LEFT JOIN` 查询扁平行，再由 Java 按主键分组。

这种写法的优点是关系清晰、SQL 可读、便于优化，也符合本项目“表与表之间通过关联字段连表查询”的约定。

---

## 14. Activiti 使用详解

### 14.1 自动部署

应用启动时，Activiti 会发现 `classpath:processes/` 下的 BPMN 文件。部署后会产生：

- Deployment：一次部署记录。
- Process Definition：部署后的流程定义。
- Version：流程定义版本。

管理员页面可以查看定义 ID、key、版本、部署 ID 和资源名。

### 14.2 启动流程

```java
String processInstanceId = runtimeService.startProcessInstanceByKey(
        LeaveProcessConstants.PROCESS_KEY,
        request.getId(),
        variables).getId();
```

三个参数：

1. `leaveApproval`：流程 key，启动最新可用定义。
2. `request.getId()`：业务键。
3. `variables`：流程启动变量。

启动后，流程从开始事件走到第一个用户任务并暂停。

### 14.3 查询我的待办

```java
List<Task> tasks = taskService.createTaskQuery()
        .taskCandidateOrAssigned(username)
        .active()
        .orderByTaskCreateTime()
        .desc()
        .list();
```

核心不是“查询全部再隐藏”，而是让查询条件本身限定当前账号。

### 14.4 签收任务

```java
taskService.setAssignee(taskId, username);
```

签收后，任务办理人变为当前账号。

### 14.5 完成任务

```java
taskService.complete(task.getId(), variables);
```

`complete()` 会结束当前任务，让流程继续走向下一个节点。如果下一个节点是用户任务，流程会再次暂停；如果到达结束事件，流程实例结束。

### 14.6 为什么要更新两处变量

核心代码：

```java
taskService.setVariables(task.getId(), variables);
runtimeService.setVariables(processInstanceId, variables);
taskService.complete(task.getId(), variables);
```

Activiti 支持任务局部变量。如果任务上存在同名局部变量，只修改流程实例变量，网关仍可能读到旧值。

本项目同时处理：

- 任务局部变量：保证本次完成任务后网关判断正确。
- 流程实例变量：保证后续节点和结束监听器读到最新结果。
- `complete()` 参数：完成任务时传递本次办理数据。

### 14.7 经理办理时设置什么

```java
variables.put("approveManager", form.isApproved());
variables.put("returnToEmployee", false);
variables.put("needHr", needsHr(request));
variables.put("needDirector", request.getLeaveDays().doubleValue() >= 5.0D);
```

随后网关读取 `approveManager` 和 `returnToEmployee` 选择出口。

### 14.8 退回后重新提交

`resubmit()` 会：

1. 确认当前任务是 `employeeResubmit`。
2. 确认操作人是申请人。
3. 重新校验表单和余额。
4. 更新业务申请。
5. 重新计算 `needHr`、`needDirector`。
6. 重置审批变量。
7. 完成补充任务，流程回到经理节点。

因此“5 天年假被退回后改为 2 天事假”会重新走经理节点，并跳过 HR、总监。

### 14.9 流程结束监听器

BPMN：

```xml
<activiti:executionListener event="end"
                            delegateExpression="${leaveEndListener}"/>
```

[LeaveEndListener.java](src/main/java/com/example/active7/workflow/LeaveEndListener.java) 负责：

1. 从业务键找到请假申请。
2. 读取 `outcome` 和 `lastComment`。
3. 使用 `notIn(APPROVED, REJECTED, WITHDRAWN)` 条件更新终态。
4. 如果更新行数为 0，直接返回，避免重复处理。
5. 最终通过的请假如果是年假，原子扣减余额。
6. 插入 `END` 业务轨迹。

结束监听器中的异常会向流程传播，并可能导致当前事务回滚，避免出现“流程已结束但余额没扣”的不一致状态。

### 14.10 如何安全修改流程阈值

本项目的 HR 阈值在 Java 的 `LeaveService.needsHr()` 中计算，BPMN 只读取 `needHr`。

例如把“大于 3 天”改为“大于 2 天”，应修改：

```java
if (request.getLeaveDays().doubleValue() > 2.0D) {
    return true;
}
```

然后重新编译、重启，再提交 3 天事假观察是否进入 HR。

不要只改 BPMN 条件表达式中的变量名或只改一段 Java，却忽略另一处。变量由 Java 计算，条件由 BPMN 判断，两边必须配套。

---

## 15. Spring Security、双 Token 与 RBAC

配置位于 [SecurityConfig.java](src/main/java/com/example/active7/config/SecurityConfig.java)，用户加载位于 [DemoUserDetailsService.java](src/main/java/com/example/active7/security/DemoUserDetailsService.java)。

### 15.1 无状态认证

应用不使用 Session、表单登录、HTTP Basic 和 CSRF Token：

- 会话创建策略为 `STATELESS`。
- 浏览器登录后保存 Access Token，业务请求携带 `Authorization: Bearer ...`。
- Refresh Token 通过 HttpOnly Cookie 保存，JavaScript 不能读取。
- 退出、登录和刷新都走 `/api/auth/**` JSON 接口。

每个业务请求都会验证 Access Token，并根据 token 中的用户 ID 重新查询数据库中的用户、角色和权限。因此角色或权限修改后，下一次请求就会按最新关系计算。

### 15.2 双 Token 机制

| Token | 形式 | 默认有效期 | 保存方式 | 用途 |
|---|---|---|---|---|
| Access Token | JWT，HS256 签名 | 15 分钟 | 浏览器 `sessionStorage` | 调用业务接口 |
| Refresh Token | 32 字节安全随机数，Base64URL 编码 | 7 天 | HttpOnly、SameSite=Strict Cookie | 刷新 Access Token |

Access Token 包含签发者、用户 ID、账号、类型和过期时间。服务端验证签名、签发者以及 `type=access`，防止把其他 JWT 当作访问令牌。

Refresh Token 不是 JWT。数据库只保存它的 SHA-256 哈希，原始值只返回给客户端。刷新时采用轮换机制：

1. 查询 Refresh Token 哈希记录。
2. 检查是否存在、未撤销、未过期。
3. 使用带条件的原子 `UPDATE` 领取旧令牌。
4. 撤销旧令牌并创建新的 Refresh Token。
5. 返回新的 Access Token 和 Refresh Token。

如果旧令牌被重复使用，服务端会撤销该用户全部有效 Refresh Token，要求重新登录，以降低令牌被盗后的持续使用风险。账号被禁用时，刷新请求也会撤销该用户全部有效 Refresh Token；禁用期间的 Access Token 会在每次请求重新加载用户时被拒绝。

浏览器在 Access Token 剩余不足 30 秒时主动刷新；业务请求收到 401 时也会尝试刷新并重试一次。并发刷新共用一个 Promise，避免多个请求同时轮换令牌。

### 15.3 接口访问规则

接口检查权限编码，不再统一使用 `hasRole("ADMIN")`：

| 接口范围 | 所需权限 |
|---|---|
| `POST /api/leave/requests` | `leave:create` |
| `GET /api/leave/requests` | `leave:view:all` |
| `GET /api/leave/requests/**` | `leave:view:mine` 或 `leave:view:all` |
| `POST /api/leave/requests/{requestId}/withdraw` | `leave:withdraw` |
| `GET /api/leave/balance` | `leave:view:balance` |
| `GET /api/leave/tasks/**` | `leave:task:view` |
| `POST /api/leave/tasks/{taskId}/claim` | `leave:task:claim` |
| `POST /api/leave/tasks/{taskId}/approve` | `leave:task:approve` |
| `POST /api/leave/tasks/{taskId}/resubmit` | `leave:task:resubmit` |
| `GET /api/admin/rbac/**` | `rbac:view` |
| `PUT /api/admin/rbac/**` | `rbac:manage` |
| `POST /api/admin/deploy/zip` | `process:deploy` |
| 其他 `GET /api/admin/**` | `process:view` |

数据库中的角色编码例如 `ADMIN`，加载后会转换为 `ROLE_ADMIN`；真正的接口授权主要使用 `leave:create`、`process:view` 等细粒度权限。未登录返回 401，已登录但权限不足返回 403。

### 15.4 任务归属校验

查询待办使用 `taskCandidateOrAssigned(username)`。

直接访问任务详情、签收或完成任务时，`visibleTask()` 会再次查询任务归属。即使别人猜到任务 ID，没有候选或办理人关系也会得到 403。Spring Security 的权限检查与 Activiti 的任务归属检查缺一不可。

### 15.5 数据权限

- 我的申请：按 `applicant = 当前用户名` 查询。
- 全部申请：需要 `leave:view:all`。
- 申请详情和轨迹：申请人本人或具有 `leave:view:all` 的用户可查看。
- 撤回：只有申请人本人可以操作。
- 待办办理：候选账号或已签收账号可以操作。
- RBAC 修改：需要 `rbac:manage`，并受管理员保护规则约束。

### 15.6 RBAC 管理保护

[RbacService.java](src/main/java/com/example/active7/service/rbac/RbacService.java) 使用事务替换关系：

- 替换用户角色前验证用户和角色存在。
- 替换角色权限前验证角色和权限存在。
- 不能删除系统中最后一个管理员。
- `ADMIN` 角色必须保留 `auth:view`、`leave:view:all`、`process:view`、`process:deploy`、`rbac:view`、`rbac:manage`。

这些保护用于避免教学项目因误操作而完全失去管理入口。

---

## 16. 页面与接口清单

页面地址：

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/active/` | 重定向到工作台 |
| GET | `/active/login` | 登录页 |
| GET | `/active/index` | 工作台 |

JSON 接口统一使用 `ResultBody`：

```json
{
  "code": 200,
  "message": "操作成功",
  "result": "实际数据"
}
```

业务数据在 `result` 中，不在 `data` 中。

### 16.1 登录信息

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 账号密码登录，返回 Access Token，同时设置 Refresh Cookie |
| POST | `/api/auth/refresh` | 轮换 Refresh Token，需要 `X-Refresh-Request: true` |
| POST | `/api/auth/logout` | 撤销当前 Refresh Token 并清除 Cookie |
| GET | `/api/auth/me` | 当前用户、角色和权限 |

登录请求体：

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

登录和刷新响应中的 `result` 包含：

```json
{
  "accessToken": "JWT 字符串",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "roles": ["ROLE_EMPLOYEE"],
  "permissions": ["auth:view", "leave:create"],
  "user": {
    "userId": "1001",
    "username": "zhangsan",
    "displayName": "张三",
    "department": "销售部"
  }
}
```

Refresh Token 不在 JSON 响应中返回，而是通过 `Set-Cookie` 设置。

### 16.2 请假业务

| 方法 | 地址 | 说明 |
|---|---|---|
| POST | `/api/leave/requests` | 提交请假 |
| GET | `/api/leave/requests` | 全部申请，需要 `leave:view:all` |
| GET | `/api/leave/requests/mine` | 我的申请 |
| GET | `/api/leave/requests/{requestId}` | 申请详情 |
| GET | `/api/leave/requests/{requestId}/records` | 审批轨迹 |
| POST | `/api/leave/requests/{requestId}/withdraw` | 撤回申请 |
| GET | `/api/leave/balance` | 我的年假余额 |
| GET | `/api/leave/tasks/mine` | 我的待办 |
| GET | `/api/leave/tasks/{taskId}` | 任务详情 |
| POST | `/api/leave/tasks/{taskId}/claim` | 签收任务 |
| POST | `/api/leave/tasks/{taskId}/approve` | 同意、拒绝、退回、确认 |
| POST | `/api/leave/tasks/{taskId}/resubmit` | 退回后重新提交 |

### 16.3 管理员接口

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/api/admin/deployments` | 部署记录 |
| GET | `/api/admin/definitions` | 流程定义 |
| GET | `/api/admin/tasks` | 全部活跃任务 |
| GET | `/api/admin/process-xml/{definitionId}` | 查看指定定义的 XML |
| POST | `/api/admin/deploy/zip` | 上传 ZIP/BAR 动态部署 |

动态部署接口位于 [ActivitiController.java](src/main/java/com/example/active7/controller/ActivitiController.java)，其他观察接口位于 [ProcessAdminController.java](src/main/java/com/example/active7/controller/ProcessAdminController.java)。

### 16.4 RBAC 管理接口

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/api/admin/rbac/users` | 用户及角色列表 |
| GET | `/api/admin/rbac/roles` | 角色及权限列表 |
| GET | `/api/admin/rbac/permissions` | 全部权限定义 |
| PUT | `/api/admin/rbac/users/{userId}/roles` | 替换用户角色，请求体为角色编码数组 |
| PUT | `/api/admin/rbac/roles/{roleCode}/permissions` | 替换角色权限，请求体为权限编码数组 |

例如为张三分配员工和经理角色：

```json
["EMPLOYEE", "MANAGER"]
```

为经理角色配置权限：

```json
["auth:view", "leave:create", "leave:view:mine", "leave:task:view", "leave:task:claim", "leave:task:approve"]
```

接口位于 [SysRbacController.java](src/main/java/com/example/active7/controller/SysRbacController.java)，关系替换逻辑位于 [RbacService.java](src/main/java/com/example/active7/service/rbac/RbacService.java)。

### 16.5 请求参数

提交和重新提交都使用 `LeaveRequestForm`：

| 字段 | 类型 | 必填 | 示例 |
|---|---|---|---|
| `leaveType` | String | 是 | `PERSONAL` |
| `startTime` | LocalDateTime | 是 | `2026-12-10T09:00:00` |
| `endTime` | LocalDateTime | 是 | `2026-12-10T18:00:00` |
| `leaveDays` | BigDecimal | 是 | `1.0` |
| `reason` | String | 是 | `学习 Activiti 流程接口` |
| `hasMedicalProof` | boolean | 否 | `false` |

审批使用 [ApprovalForm.java](src/main/java/com/example/active7/service/dto/ApprovalForm.java)：

| 字段 | 类型 | 说明 |
|---|---|---|
| `approved` | boolean | 是否同意 |
| `returnToApplicant` | boolean | 是否退回，仅经理可用 |
| `comment` | String | 审批意见，拒绝和退回时必填 |

员工确认时仍调用 `approve` 接口，由任务节点 `employeeConfirm` 决定实际动作。

---

## 17. 使用 curl 调试接口

下文默认应用运行在：

```text
http://localhost:8890/active
```

为了减少重复地址，可以先设置变量。变量只用于当前终端会话，不写入配置文件：

```bash
BASE=http://localhost:8890/active
```

### 17.1 登录并获取 Access Token

```bash
curl -i \
  -c /tmp/active7-zhangsan.cookie \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "123456"
  }' \
  "$BASE/api/auth/login"
```

响应中的 `result.accessToken` 就是 Access Token。`Set-Cookie` 中会设置 `active7_refresh`，它只保存在 Cookie 文件里，不出现在 JSON 响应中。

把 Access Token 保存到当前终端变量：

```bash
ACCESS_TOKEN=替换为登录响应中的accessToken
```

后续业务请求使用：

```bash
-H "Authorization: Bearer $ACCESS_TOKEN"
```

### 17.2 查询当前用户

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$BASE/api/auth/me"
```

预期能看到：

```json
{
  "code": 200,
  "message": "操作成功",
  "result": {
    "userId": "1001",
    "username": "zhangsan",
    "displayName": "张三",
    "department": "销售部",
    "roles": ["ROLE_EMPLOYEE"],
    "permissions": ["auth:view", "leave:create", "leave:view:mine"]
  }
}
```

### 17.3 刷新并轮换 Token

```bash
curl -i \
  -b /tmp/active7-zhangsan.cookie \
  -c /tmp/active7-zhangsan.cookie \
  -H "X-Refresh-Request: true" \
  -X POST \
  "$BASE/api/auth/refresh"
```

`-b` 读取旧 Cookie，`-c` 把新 Cookie 写回同一个文件。刷新成功后，旧 Refresh Token 已撤销，必须使用新 Cookie。响应会返回新的 Access Token，更新 `ACCESS_TOKEN` 后再调用业务接口。

如果重复使用旧 Cookie 文件中的令牌，服务端会返回 401，并撤销该用户全部有效 Refresh Token。此时必须重新登录。

### 17.4 提交请假

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "leaveType": "PERSONAL",
    "startTime": "2026-12-10T09:00:00",
    "endTime": "2026-12-10T18:00:00",
    "leaveDays": 1.0,
    "reason": "学习 Activiti 流程接口",
    "hasMedicalProof": false
  }' \
  "$BASE/api/leave/requests"
```

返回的 `result` 是 `requestId`，后续查询申请和轨迹会用到。

### 17.5 查询王五的待办

先使用王五账号登录，并把返回的 Access Token 保存为 `MANAGER_ACCESS_TOKEN`：

```bash
curl -i \
  -c /tmp/active7-wangwu.cookie \
  -H "Content-Type: application/json" \
  -d '{
    "username": "wangwu",
    "password": "123456"
  }' \
  "$BASE/api/auth/login"
```

查询任务：

```bash
curl -i \
  -H "Authorization: Bearer $MANAGER_ACCESS_TOKEN" \
  "$BASE/api/leave/tasks/mine"
```

从 `result` 中找到 `taskId`。

### 17.6 签收任务

```bash
curl -i \
  -H "Authorization: Bearer $MANAGER_ACCESS_TOKEN" \
  -X POST \
  "$BASE/api/leave/tasks/替换为任务ID/claim"
```

### 17.7 经理同意

```bash
curl -i \
  -H "Authorization: Bearer $MANAGER_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "returnToApplicant": false,
    "comment": "同意"
  }' \
  -X POST \
  "$BASE/api/leave/tasks/替换为任务ID/approve"
```

### 17.8 经理退回

```bash
curl -i \
  -H "Authorization: Bearer $MANAGER_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": false,
    "returnToApplicant": true,
    "comment": "请补充工作交接安排"
  }' \
  -X POST \
  "$BASE/api/leave/tasks/替换为任务ID/approve"
```

### 17.9 张三重新提交

```bash
curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "leaveType": "PERSONAL",
    "startTime": "2026-12-11T09:00:00",
    "endTime": "2026-12-11T18:00:00",
    "leaveDays": 1.0,
    "reason": "已补充工作交接安排",
    "hasMedicalProof": false
  }' \
  -X POST \
  "$BASE/api/leave/tasks/替换为补充任务ID/resubmit"
```

### 17.10 动态部署流程包

将 BPMN 文件直接放在 ZIP 根目录，不要套一层无用的目录：

```bash
(cd springboot-active7/src/main/resources/processes \
  && zip active7-practice.zip leave-approval.bpmn20.xml)
```

管理员先登录，并把返回的 Access Token 保存为 `ADMIN_ACCESS_TOKEN`：

```bash
curl -i \
  -c /tmp/active7-admin.cookie \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' \
  "$BASE/api/auth/login"
```

再从仓库根目录上传：

```bash
curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  -F "file=@springboot-active7/src/main/resources/processes/active7-practice.zip" \
  "$BASE/api/admin/deploy/zip"
```

执行上传命令时请保持当前目录为仓库根目录；如果使用其他目录，请把 `file=@...` 改成本地实际路径。

教学版动态部署没有完整的文件大小、压缩包路径和恶意内容检查，只适合本地学习。

### 17.11 查看流程 XML

接口参数是流程定义 ID，不是部署 ID：

```bash
curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  "$BASE/api/admin/process-xml/替换为流程定义ID"
```

定义 ID 通常类似：

```text
leaveApproval:1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

部署 ID 是单独的 UUID。不要把二者混用。

### 17.12 查询和修改 RBAC

查询用户、角色和权限：

```bash
curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  "$BASE/api/admin/rbac/users"

curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  "$BASE/api/admin/rbac/roles"

curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  "$BASE/api/admin/rbac/permissions"
```

为张三分配员工和经理角色：

```bash
curl -i \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -X PUT \
  -d '["EMPLOYEE", "MANAGER"]' \
  "$BASE/api/admin/rbac/users/1001/roles"
```

修改后，张三下一次请求就会按最新角色和权限重新计算。若正在浏览器中操作，页面保存成功后会重新调用 `/api/auth/me` 并刷新菜单。

### 17.13 退出登录

```bash
curl -i \
  -b /tmp/active7-zhangsan.cookie \
  -c /tmp/active7-zhangsan.cookie \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -X POST \
  "$BASE/api/auth/logout"
```

退出会撤销当前 Refresh Token 并清除 Cookie。Access Token 本身无状态，仍会在到期前有效；生产环境如需退出后立即失效，需要增加黑名单或版本校验机制。禁用账号不同，认证过滤器每次请求都会重新读取用户状态，因此禁用后会立即拒绝该账号的 Access Token。

---

## 18. 使用 SQL 观察流程

连接数据库：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p springboot_active7
```

下面的查询都是只读操作，适合学习。

### 18.1 查看业务表

```sql
SHOW TABLES;
```

### 18.2 查看表和字段注释

```sql
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;

SHOW FULL COLUMNS FROM leave_request;
```

### 18.3 查看请假申请

```sql
SELECT id, applicant, leave_type, leave_days, status,
       current_node, process_instance_id, created_at, updated_at
FROM leave_request
ORDER BY created_at DESC;
```

观察重点：

- `status` 是业务状态。
- `current_node` 是当前停下的节点。
- `process_instance_id` 把业务表和引擎实例关联起来。

### 18.4 查看业务轨迹

```sql
SELECT record_seq, request_id, node, action, operator, comment, created_at
FROM leave_record
ORDER BY record_seq;
```

`created_at` 只精确到秒，同一秒内的多条轨迹不能只按时间排序。`record_seq` 是数据库自增顺序号，页面和接口都按它升序展示，保证“提交 → 签收 → 审批 → 确认 → 结束”的顺序稳定。

常见 `action`：

- `SUBMIT`：提交。
- `CLAIM`：签收。
- `APPROVE`：同意。
- `RETURN`：退回。
- `RESUBMIT`：重新提交。
- `REJECT`：拒绝。
- `WITHDRAW`：撤回。
- `CONFIRM`：员工确认。
- `END`：流程结束。

### 18.5 查看年假余额

```sql
SELECT user_id, balance_year, annual_days, used_days,
       annual_days - used_days AS remaining_days
FROM leave_balance
WHERE user_id = '1001';
```

`1001` 是张三的用户 ID。

### 18.6 查看当前任务

```sql
SELECT id_, name_, assignee_, process_instance_id_, task_def_key_
FROM act_ru_task
ORDER BY create_time_ DESC;
```

完成任务后，这条任务会从运行时表消失，并进入历史任务表。

### 18.7 查看流程实例

```sql
SELECT id_, proc_def_id_, business_key_, start_time_, end_time_
FROM act_hi_procinst
ORDER BY start_time_ DESC;
```

`business_key_` 对应请假申请 ID。

### 18.8 查看运行时变量

```sql
SELECT name_, text_, long_, double_, proc_inst_id_
FROM act_ru_variable
ORDER BY proc_inst_id_, name_;
```

流程结束后，运行时变量可能已经清理，可以改查 `act_hi_varinst`。

### 18.9 查看历史任务

```sql
SELECT proc_inst_id_, task_def_key_, name_, assignee_,
       start_time_, end_time_
FROM act_hi_taskinst
ORDER BY start_time_ DESC;
```

### 18.10 推荐观察顺序

1. 张三提交前记录一次表状态。
2. 提交后查 `leave_request`、`act_ru_task`、`act_ru_variable`。
3. 王五签收后查 `act_ru_task.assignee_`。
4. 王五同意后查下一个任务和 `leave_request.current_node`。
5. 张三确认后查 `act_hi_procinst.end_time_`、业务状态和余额。
6. 对比 `leave_record` 与 `act_hi_taskinst`，理解业务轨迹和引擎历史的区别。

---

## 19. 编译、打包和运行验证

项目不新增自动化测试代码，通过编译、打包、启动和实际页面操作验证功能。

### 19.1 编译打包

```bash
mvn -pl springboot-active7 -am clean package
```

看到 `BUILD SUCCESS` 表示模块及其依赖可以正常构建。

### 19.2 检查产物

```bash
ls -lh springboot-active7/target/active7.jar
```

### 19.3 启动

确认 MySQL 已启动、数据库已创建、连接信息已填写，然后执行：

```bash
java -jar springboot-active7/target/active7.jar
```

### 19.4 页面验收清单

- [ ] 登录页能显示 6 个演示身份。
- [ ] 张三能提交请假。
- [ ] 李四看不到张三的员工确认任务。
- [ ] 王五能看到经理候选任务。
- [ ] 王五签收后，其他账号不能代替完成。
- [ ] 1 天事假跳过 HR 和总监。
- [ ] 0.5 天病假进入 HR。
- [ ] 5 天年假经过经理、HR、总监和员工确认。
- [ ] 经理退回后，张三可以修改并重新提交。
- [ ] 最终通过后，年假余额正确扣减。
- [ ] 管理员能看到流程定义和 XML。
- [ ] 普通员工访问流程管理接口返回 403。
- [ ] 张三登录后获得 Access Token 和 HttpOnly Refresh Cookie。
- [ ] Access Token 过期后可以刷新，旧 Refresh Token 不能重复使用。
- [ ] 管理员可以分配用户角色和角色权限，下一次请求按新权限执行。

修改 BPMN、Java 或静态资源后，需要重新打包并重启。MySQL 数据会跨重启保留。

---

## 20. 新手练习剧本

### 练习 1：短假跳过 HR 和总监

- 账号：张三。
- 内容：1 天事假。
- 预期：经理 → 员工确认。

### 练习 2：特殊假期进入 HR

- 账号：张三。
- 内容：0.5 天病假。
- 预期：经理 → HR → 员工确认。

### 练习 3：长假全链路

- 账号：张三。
- 内容：5 天年假。
- 预期：经理 → HR → 总监 → 员工确认。
- 最终余额减少 5 天。

### 练习 4：退回后重新计算分支

1. 张三提交 5 天年假。
2. 王五退回补充。
3. 张三改为 2 天事假并重新提交。
4. 预期：重新从经理开始，跳过 HR 和总监。

### 练习 5：撤回

1. 张三提交申请。
2. 王五尚未签收。
3. 张三撤回。
4. 预期：状态为 `WITHDRAWN`，余额不变。

### 练习 6：拒绝

1. 张三提交申请。
2. 王五签收后拒绝并填写意见。
3. 预期：状态为 `REJECTED`，流程结束，余额不变。

### 练习 7：验证任务隔离

1. 张三提交请假。
2. 李四的待办应为 0。
3. 李四尝试访问王五的任务详情。
4. 预期：403。

### 练习 8：验证流程管理权限

1. 张三访问 `/active/api/admin/definitions`。
2. 预期：403，因为张三没有 `process:view`。
3. 管理员访问同一接口。
4. 预期：200，并返回流程定义。

### 练习 9：观察变量变化

1. 经理办理前查询 `act_ru_variable`。
2. 经理同意后再次查询。
3. 对比 `approveManager`、`needHr`、`needDirector`。
4. 查看下一个任务节点是否与变量一致。

### 练习 10：验证双 Token 轮换

1. 登录张三账号，保存 Cookie 文件。
2. 调用刷新接口，观察返回新的 Access Token 和 `Set-Cookie`。
3. 使用刷新前的旧 Cookie 再调用一次。
4. 预期：401，并提示 Refresh Token 已被使用或撤销。

### 练习 11：验证 RBAC 即时生效

1. 管理员给张三临时分配 `MANAGER` 角色。
2. 张三下一次请求后，`/api/auth/me` 中应出现经理角色及任务权限。
3. 移除 `MANAGER` 角色后，张三下一次请求不再具有经理任务权限。
4. 尝试移除最后一个管理员或删除管理员必需权限，预期被拒绝。

---

## 21. 常见错误与排查

### 21.1 页面能打开，但接口返回 401

原因：

- 未登录或 Access Token 已过期。
- 请求没有携带 `Authorization: Bearer ...`。
- JWT 签名、签发者或令牌类型不正确。
- Refresh Token 已过期、已撤销或发生重复使用。
- 刷新请求缺少 `X-Refresh-Request: true`。

处理：

- 重新登录并更新 Access Token。
- 确认请求地址包含 `/active`。
- curl 使用 `-H "Authorization: Bearer $ACCESS_TOKEN"`，不要使用 `-u`。
- 刷新时同时使用 `-b` 和 `-c` 更新 Cookie 文件。
- 如果提示 Refresh Token 重复使用，重新登录，不要继续使用旧 Cookie。

### 21.2 POST 返回 403

检查当前账号是否具有接口所需权限，例如：

- 提交请假：`leave:create`。
- 签收任务：`leave:task:claim`。
- 办理审批：`leave:task:approve`。
- 动态部署：`process:deploy`。
- 修改 RBAC：`rbac:manage`。

具有权限编码但任务仍返回 403 时，继续检查 Activiti 的候选人或办理人。RBAC 修改后，下一次请求会重新加载权限；浏览器页面也会在保存后刷新当前身份。

### 21.3 管理员看不到流程 XML

检查请求中的 ID 是流程定义 ID，不是部署 ID。

接口通过 `ProcessDefinition.getResourceName()` 获取真实资源名，避免硬编码资源路径。

### 21.4 经理同意后流程直接结束

检查：

- `approveManager` 是否正确设置为 `true`。
- `returnToEmployee` 是否为 `false`。
- 是否同时更新任务局部变量和流程实例变量。
- BPMN 条件变量名是否与 Java 一致。

### 21.5 员工没有确认任务

检查：

- 经理、HR、总监是否全部同意。
- `applicant` 是否是正确登录账号。
- 当前登录账号是否与申请人一致。
- 是否错误地修改了网关默认出口。

### 21.6 流程结束但业务状态不对

检查：

- 结束监听器是否被调用。
- `outcome` 和 `lastComment` 是否写入流程实例变量。
- 业务键是否能找到 `leave_request`。
- 查看日志中是否有监听器异常。

### 21.7 年假余额没有扣减

检查：

- 最终状态是否真的是 `APPROVED`。
- 请假类型是否为 `ANNUAL`。
- 当前年份是否存在对应余额记录。
- `addUsedDays` 的条件是否满足。

### 21.8 重启后数据不见了

MySQL 模式下数据应持久保存。检查：

- 是否连接了不同 MySQL 实例。
- 数据库名是否正确。
- 是否错误地执行了清理脚本。
- 启动日志是否激活 `mysql` profile。

### 21.9 提示表不存在

检查：

- `SHOW TABLES` 中的实际表名。
- 是否手动创建了数据库。
- Activiti 建表日志是否成功。
- Linux 下表名大小写是否匹配。

### 21.10  Activiti 7 找不到 `IdentityService`

这是版本差异。Activiti 7.1.0.M6 不再提供旧版那套 `IdentityService`、`Group`、`User`、`Membership` API。

本项目使用：

- 自定义 `sys_user`、`sys_role`、`sys_user_role` 保存账号。
- Spring Security 管理登录身份。
- BPMN 的 `candidateUsers`、`assignee` 关联流程任务。

不要把 Activiti 5/6 的身份表教程直接套到 Activiti 7。

### 21.11 接口返回 500，但页面只显示“服务异常”

查看后端日志中的完整堆栈。教学版全局异常处理会把异常消息放进响应，生产环境应避免把内部细节直接暴露给客户端。

---

## 22. 业务状态说明

| 状态 | 含义 |
|---|---|
| `SUBMITTED` | 已提交，等待经理 |
| `RETURNED` | 被退回，等待员工补充 |
| `MANAGER_APPROVED` | 经理已同意，等待 HR |
| `HR_APPROVED` | HR 已同意，等待总监或员工确认 |
| `CONFIRM_PENDING` | 审批通过，等待员工确认 |
| `APPROVED` | 最终通过 |
| `REJECTED` | 被拒绝 |
| `WITHDRAWN` | 被撤回 |

`current_node` 表示当前流程节点，`status` 表示业务展示状态，两者不要混用。

---

## 23. 从教学项目走向生产环境

生产化前至少还要考虑：

- 真实部门、上下级、代理审批和会签/或签关系。
- 流程定义版本管理、审批中实例升级策略和灰度发布。
- 动态部署文件大小限制、路径校验和病毒扫描。
- 数据库账号最小权限、备份、监控和审计。
- 多实例部署时配置合适的异步执行器。
- 余额并发扣减、重复请求和分布式事务。
- 消息通知、催办、超时升级、节假日和工时计算。
- 医疗证明等附件的安全存储与访问控制。
- 日志脱敏、密码策略、登录失败限制和验证码。
- JWT 密钥安全存储与轮换、Refresh Token 定期清理、Access Token 立即失效策略。
- HTTPS、Secure Cookie、同源策略和前端令牌存储风险。
- 更细粒度的数据权限，例如经理只能查看本部门申请。
- 按请假所属年度计算年假，而不是只使用当前自然年份。
- 移除或严格限制演示账号初始化逻辑。
- 对流程表结构升级、历史数据归档和清理制定方案。

---

## 24. 源码阅读顺序与术语表

### 24.1 推荐阅读顺序

1. [application.yml](src/main/resources/application.yml)：端口、数据源 profile、MyBatis-Plus、Activiti 和 JWT 配置。
2. [leave-approval.bpmn20.xml](src/main/resources/processes/leave-approval.bpmn20.xml)：流程整体结构。
3. [SecurityConfig.java](src/main/java/com/example/active7/config/SecurityConfig.java)：无状态认证和接口权限规则。
4. [JwtTokenProvider.java](src/main/java/com/example/active7/security/JwtTokenProvider.java) 与 [JwtAuthenticationFilter.java](src/main/java/com/example/active7/security/JwtAuthenticationFilter.java)：Access Token 的创建、验证和请求认证。
5. [RefreshTokenService.java](src/main/java/com/example/active7/service/RefreshTokenService.java)：Refresh Token 哈希存储、轮换、撤销和重复使用检测。
6. [AuthController.java](src/main/java/com/example/active7/controller/AuthController.java) 与 [AuthService.java](src/main/java/com/example/active7/service/AuthService.java)：登录、刷新、退出和令牌签发。
7. [DemoUserDetailsService.java](src/main/java/com/example/active7/security/DemoUserDetailsService.java)：通过联表查询加载角色和权限。
8. [SysAuthorityMapper.xml](src/main/resources/mapper/SysAuthorityMapper.xml) 与 [RbacService.java](src/main/java/com/example/active7/service/rbac/RbacService.java)：RBAC 关系查询和管理。
9. [LeaveController.java](src/main/java/com/example/active7/controller/LeaveController.java)：业务接口入口。
10. [LeaveService.java](src/main/java/com/example/active7/service/LeaveService.java)：流程启动、待办查询和任务办理，是全书重点。
11. [LeaveEndListener.java](src/main/java/com/example/active7/workflow/LeaveEndListener.java)：流程结束后的业务同步。
12. [schema-mysql.sql](src/main/resources/db/schema-mysql.sql)：业务表、RBAC 表、Refresh Token 表和字段注释。
13. [LeaveRequest.java](src/main/java/com/example/active7/model/LeaveRequest.java) 与 [LeaveRequestMapper.java](src/main/java/com/example/active7/mapper/LeaveRequestMapper.java)：MyBatis-Plus 映射。
14. [index.html](src/main/resources/templates/index.html) 与 [app.js](src/main/resources/static/js/app.js)：登录、自动续期、权限菜单和 RBAC 页面。

阅读时始终带着三个问题：流程停在哪里、任务属于谁、下一个节点由什么变量决定。

### 24.2 术语表

| 术语 | 含义 |
|---|---|
| BPMN | 业务流程建模标记法，用 XML 描述流程 |
| 流程定义 | 可复用的流程模板 |
| 流程实例 | 一次具体的流程执行 |
| 用户任务 | 需要人办理的任务 |
| 候选用户 | 有资格签收任务的账号 |
| 办理人 | 已经指定或签收任务的账号 |
| 排他网关 | 从多个出口中选择一个 |
| 流程变量 | 流程执行期间保存的数据 |
| 任务局部变量 | 只在某个任务范围内有效的变量 |
| 业务键 | 关联业务记录和流程实例的键 |
| 历史记录 | 流程引擎保存的过去执行数据 |
| Access Token | 短期 JWT，通过 Bearer 请求头调用业务接口 |
| Refresh Token | 长期不透明随机令牌，通过 HttpOnly Cookie 保存并用于轮换 |
| Token 轮换 | 刷新时撤销旧 Refresh Token 并签发新令牌 |
| RBAC | 用户、角色、权限和关系表组成的基于角色的访问控制 |
| 权限编码 | 接口实际检查的细粒度字符串，例如 `leave:create` |
| SHA-256 | 本项目用于保存 Refresh Token 哈希的单向哈希算法 |
| BCrypt | 密码哈希算法 |

掌握这些概念后，再学习 Activiti 的会签、或签、子流程、事件、超时和异步执行，就会容易很多。
