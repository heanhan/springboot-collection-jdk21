# OAuth Auth 授权中心 · 接入说明

本服务是基于 Spring Boot 2.7.18 的 **RBAC + 自定义 JWT（RS256）授权中心**。
本文档说明：资源服务器如何接入验签、客户端如何获取令牌、以及按需集成 spring-cloud 的方案。

---

## 一、架构要点（路线 B）

- 已移除 `@EnableAuthorizationServer`（`spring-security-oauth2` 已 EOL），**统一以自定义 `TokenService` 为唯一 Token 出口**，消除双轨。
- Access/Refresh 双 Token 使用 **RS256 非对称签名**（RSA 2048）：授权中心持私钥签发，资源服务器持公钥验签。
- 公钥通过 **JWK 端点**对外暴露，资源服务器可离线验签，无需回调授权中心。
- 授权落地：基于 `sys_permission` 的 `url + method -> perm_code` 规则，由 `DynamicAuthorizationFilter` 动态比对。

---

## 二、对外端点

| 端点 | 方法 | 说明 | 是否需认证 |
| --- | --- | --- | --- |
| `/auth/login` | POST | 用户名密码登录（JSON），返回双 Token | 否 |
| `/auth/refresh` | POST | 刷新 Token（滑动续期） | 否 |
| `/auth/logout` | POST | 登出（Token 加入黑名单） | 否 |
| `/auth/current` | GET | 当前用户信息 | 是 |
| `/oauth/token` | POST | 客户端令牌（`client_credentials`） | 否（凭 client_secret） |
| `/oauth/token_key` | GET | JWK 公钥集合（供资源服务器验签） | 否 |
| `/.well-known/jwks.json` | GET | 同上（标准路径别名） | 否 |
| `/admin/users/**` | * | 用户管理 CRUD + 分配角色 | 是（需权限） |
| `/admin/roles/**` | * | 角色管理 CRUD + 分配权限/菜单 | 是（需权限） |
| `/admin/permissions/**` | * | 权限管理 CRUD | 是（需权限） |
| `/admin/menus/**` | * | 菜单管理 CRUD（树形） | 是（需权限） |
| `/admin/clients/**` | * | OAuth 客户端管理 CRUD | 是（需权限） |

### JWK 响应示例

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "oauth-auth-rsa-key",
      "n": "<Base64URL 模数>",
      "e": "<Base64URL 指数>"
    }
  ]
}
```

### JWT Claims 说明

用户令牌（`tokenType=access|refresh`）：`userId`、`username`、`nickname`、`roles`、`permissions`、`tokenType`、`jti(setId)`、`sub=userId`。

客户端令牌（`tokenType=client`）：`clientId`、`scopes`、`tokenType`、`jti`、`sub=clientId`。

---

## 三、RSA 密钥配置

`application.yml`：

```yaml
jwt:
  key-id: oauth-auth-rsa-key
  rsa:
    private-key:   # Base64(PKCS#8) 私钥；留空则启动自动生成（仅开发，重启失效）
    public-key:    # Base64(X.509) 公钥
  access-expire: 1800     # Access Token 30 分钟
  refresh-expire: 604800  # Refresh Token 7 天
```

> **生产环境务必配置固定密钥**，否则重启后旧令牌全部失效、JWK 变更。
> 生成方式（OpenSSL）：
> ```bash
> openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
> openssl pkcs8 -topk8 -nocrypt -in private.pem -out private_pkcs8.pem
> openssl rsa -in private.pem -pubout -out public.pem
> # 去掉 PEM 头尾与换行后即为 Base64 字符串，分别填入 private-key / public-key
> ```

---

## 四、资源服务器接入（RS256 离线验签）

资源服务器无需依赖本服务源码，只需按标准 JWT + JWK 验签。以 Spring Boot 2.7 + Spring Security Resource Server 为例：

### 1. 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 2. 配置 JWK 地址

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://oauth-auth-service:9000/oauth/token_key
```

### 3. 权限映射（把 JWT 中 permissions 映射为 authorities）

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
    delegate.setAuthoritiesClaimName("permissions"); // 使用本中心的 permissions 声明
    delegate.setAuthorityPrefix("");                 // permCode 直接作为 authority
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(delegate);
    return converter;
}

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
}
```

之后即可在接口上使用 `@PreAuthorize("hasAuthority('user:list')")` 做方法级鉴权。

> 若资源服务器不使用 Spring Security，也可用任意 JWT 库拉取 JWK（按 `kid` 匹配 `n/e` 构造 RSA 公钥）本地验签。

---

## 五、客户端令牌（client_credentials）

多客户端通过 **JDBC 存储**（`oauth_client` 表）管理，凭 `client_id + client_secret` 获取机器令牌（M2M）。

### 获取令牌

```bash
curl -X POST "http://localhost:9000/oauth/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=demo-client" \
  -d "client_secret=admin123"
```

返回：

```json
{
  "accessToken": "<RS256 JWT>",
  "tokenType": "Bearer",
  "accessExpiresIn": 3600
}
```

该令牌同样由 RS256 私钥签名，资源服务器用同一 JWK 验签即可。

### 管理客户端

通过 `/admin/clients/**`（需 `sys:client:*` 权限）进行客户端的增删改查。新增时 `clientSecret` 传明文，服务端 BCrypt 加密存储。

---

## 六、按需集成 spring-cloud

当前服务是**独立可运行**的授权中心，未强绑定 spring-cloud。若需接入微服务体系，可按需引入：

### 1. 注册中心（Nacos 示例）

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

> 注意 spring-cloud / spring-cloud-alibaba 版本需与 Spring Boot 2.7.x 对齐
> （例如 spring-cloud 2021.0.x + spring-cloud-alibaba 2021.0.5.0）。

### 2. 网关统一鉴权（Spring Cloud Gateway）

推荐模式：**网关做「验签 + 粗粒度放行」，下游资源服务做「细粒度权限」**。

- 网关引入 `spring-boot-starter-oauth2-resource-server`，配置本中心 `jwk-set-uri` 完成 RS256 验签；
- 网关只校验令牌有效性并透传 `Authorization` 头，或将解析出的 `userId/permissions` 放入自定义请求头下传；
- 各下游服务再按 `permissions` 做 `@PreAuthorize` 方法级鉴权。

细粒度 URL-权限映射已在本授权中心内以 `sys_permission(url/method/perm_code)` + `DynamicAuthorizationFilter` 落地，网关如需同款动态鉴权，可复用该规则表思路。

---

## 七、初始化与默认账号

- 建表：`src/main/resources/sql/schema.sql`
- 种子数据：`src/main/resources/sql/data.sql`
- 默认用户：`admin / admin123`（ADMIN，全部权限）、`user / admin123`（USER，基础权限）
- 默认客户端：`demo-client / admin123`（`client_credentials`，scope=read,write）

> `spring.sql.init.mode` 默认 `never`，首次初始化可临时改为 `always` 或手动执行脚本。
