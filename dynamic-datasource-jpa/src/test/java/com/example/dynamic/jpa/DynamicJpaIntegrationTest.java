package com.example.dynamic.jpa;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.common.util.JwtUtil;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.security.LoginUser;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.dao.AuthNodeDao;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.TenantDataInfoDao;
import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.druid.url=jdbc:h2:mem:dynamic_system;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.druid.driver-class-name=org.h2.Driver",
        "spring.datasource.druid.username=sa",
        "spring.datasource.druid.password=",
        "spring.datasource.druid.initial-size=0",
        "spring.datasource.druid.min-idle=0",
        "spring.datasource.druid.max-active=4",
        "spring.datasource.druid.max-wait=1000",
        "spring.datasource.druid.test-while-idle=false",
        "spring.datasource.druid.filters=",
        "spring.datasource.druid.stat-view-servlet.enabled=false",
        "spring.datasource.druid.web-stat-filter.enabled=false",
        "security.jwt.secret=integration-test-only-secret-at-least-32-bytes",
        "app.token.store=memory",
        "app.token.access-ttl-minutes=30",
        "app.token.refresh-ttl-days=7"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DynamicJpaIntegrationTest {
    private static final String PASSWORD = "test-password";
    private static final String TEST_SECRET = "integration-test-only-secret-at-least-32-bytes";
    private static final String USER_ROLE = "ROLE_TENANT_USER";
    private static final String ADMIN_ROLE = "ROLE_TENANT_ADMIN";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TenantService tenants;
    @Autowired TenantDataInfoService sources;
    @Autowired TenantDataInfoDao sourceDao;
    @Autowired UserDao users;
    @Autowired RoleDao roles;
    @Autowired AuthNodeDao authNodes;
    @Autowired AuthNodeService authNodeService;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired @Qualifier("multipleDataSource") DataSource dataSource;
    @Autowired @Qualifier("multipleTransactionManager") PlatformTransactionManager transactionManager;

    @AfterEach
    void contextIsCleared() {
        assertThat(LoginInfoHolder.getTenant()).isNull();
    }

    @Test
    void authenticatesAndRoutesIdenticalIdsToDifferentTenantDatabases() throws Exception {
        Tenant first = tenant();
        Tenant second = tenant();
        register(first, "第一个租户");
        register(second, "第二个租户");
        String firstToken = login(user(first.getId(), USER_ROLE));
        String secondToken = login(user(second.getId(), USER_ROLE));
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/test/getTestById").param("id", "1")
                            .header("Authorization", "Bearer " + firstToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.name").value("第一个租户"));
            mvc.perform(get("/api/test/getTestById").param("id", "1")
                            .header("Authorization", "Bearer " + secondToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.name").value("第二个租户"));
        }
        mvc.perform(get("/api/test/getTestById").param("id", "1")
                        .param("data", second.getId().toString()).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnonymousWrongPasswordAndInvalidTokens() throws Exception {
        User user = user(tenant().getId(), USER_ROLE);
        mvc.perform(get("/api/test/getTestById").param("id", "1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginUser().setUsername(user.getUsername()).setPassword("wrong"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.result").isEmpty());
        String expired = JwtUtil.createToken(user.getUsername(), TEST_SECRET, LoginInfo.fromUser(user), -1000L);
        String wrongKey = JwtUtil.createToken(user.getUsername(), "another-test-secret-at-least-32-bytes", LoginInfo.fromUser(user));
        for (String token : new String[]{"Bearer malformed", "Basic invalid", "Bearer " + expired, "Bearer " + wrongKey}) {
            mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejectsLockedAccountsAndTokensWithChangedTenant() throws Exception {
        User user = user(tenant().getId(), USER_ROLE);
        String token = login(user);
        user.setTenantId(tenant().getId());
        users.saveAndFlush(user);
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        user.setStatus(2);
        users.saveAndFlush(user);
        mvc.perform(post("/api/login").contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginUser().setUsername(user.getUsername()).setPassword(PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminWritesUseSystemDatabaseAndNeverReturnDatabasePassword() throws Exception {
        Tenant tenant = tenant();
        User admin = user(tenant.getId(), ADMIN_ROLE);
        String token = login(admin);
        AddDataSourceReqVo request = sourceRequest(tenant);
        mvc.perform(post("/api/dataSource/addDataSource").contentType("application/json")
                        .header("Authorization", "Bearer " + token).content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.tenantId").value(tenant.getId()))
                .andExpect(jsonPath("$.result.password").doesNotExist());
        assertThat(sourceDao.existsByTenantIdAndIsDelFalse(tenant.getId())).isTrue();
        assertThat(router().getResolvedDataSources()).containsKey(tenant.getId());
        assertThat(((DruidDataSource) router().getResolvedDataSources().get(tenant.getId())).getActiveCount()).isZero();
    }

    @Test
    void deniesOrdinaryUserManagementAndAdminCrossTenantWrites() throws Exception {
        Tenant first = tenant();
        String ordinary = login(user(first.getId(), USER_ROLE));
        String admin = login(user(first.getId(), ADMIN_ROLE));
        AddDataSourceReqVo other = sourceRequest(tenant());
        for (String token : new String[]{ordinary, admin}) {
            mvc.perform(post("/api/dataSource/addDataSource").contentType("application/json")
                            .header("Authorization", "Bearer " + token).content(mapper.writeValueAsString(other)))
                    .andExpect(status().isForbidden());
        }
        assertThat(sourceDao.existsByTenantIdAndIsDelFalse(other.getTenantId())).isFalse();
    }

    @Test
    void tenantCrudPersistsChecksDuplicatesAndFiltersSoftDeletedRecords() {
        Tenant first = tenant();
        assertThat(first.getId()).isPositive();
        assertThat(tenants.checkMobileExists(first.getPhone())).isTrue();
        assertThat(tenants.checkMobileExists("unused-phone")).isFalse();
        Tenant duplicate = new Tenant();
        duplicate.setTenantName(first.getTenantName());
        duplicate.setPhone("another-phone");
        assertThatThrownBy(() -> tenants.addTenant(duplicate)).isInstanceOf(BaseException.class);
        duplicate.setTenantName("another-name");
        duplicate.setPhone(first.getPhone());
        assertThatThrownBy(() -> tenants.addTenant(duplicate)).isInstanceOf(BaseException.class);
        first.setTenantName("edited-" + UUID.randomUUID());
        assertThat(tenants.editTenant(first)).isTrue();
        assertThat(tenants.getTenantById(first.getId()).getTenantName()).isEqualTo(first.getTenantName());
        assertThat(tenants.listAllTenant()).extracting(Tenant::getId).contains(first.getId());
        assertThat(tenants.deleteTenant(first.getId())).isTrue();
        assertThat(tenants.getTenantById(first.getId())).isNull();
        assertThat(tenants.getTenantByName(first.getTenantName())).isNull();
        assertThat(tenants.listAllTenant()).extracting(Tenant::getId).doesNotContain(first.getId());
        assertThat(tenants.checkMobileExists(first.getPhone())).isFalse();
    }

    @Test
    void rollbackNeverPublishesTenantDataSource() {
        Tenant tenant = tenant();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            sources.addTenantDataInfo(sourceRequest(tenant));
            assertThat(router().getResolvedDataSources()).doesNotContainKey(tenant.getId());
            status.setRollbackOnly();
        });
        assertThat(router().getResolvedDataSources()).doesNotContainKey(tenant.getId());
        assertThat(sourceDao.existsByTenantIdAndIsDelFalse(tenant.getId())).isFalse();
        sources.addTenantDataInfo(sourceRequest(tenant));
        assertThat(router().getResolvedDataSources()).containsKey(tenant.getId());
        assertThatThrownBy(() -> sources.addTenantDataInfo(sourceRequest(tenant))).isInstanceOf(BaseException.class);
    }

    @Test
    void deletedTenantRevokesLoginAndRemovesDataSource() throws Exception {
        Tenant tenant = tenant();
        User user = user(tenant.getId(), USER_ROLE);
        register(tenant, "待删除租户");
        String token = login(user);
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(tenants.deleteTenant(tenant.getId())).isTrue();
        assertThat(router().getResolvedDataSources()).doesNotContainKey(tenant.getId());
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginUser().setUsername(user.getUsername()).setPassword(PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rbacAllowsOnlyRolesWithMatchingAuthNodes() throws Exception {
        Tenant tenant = tenant();
        User ordinary = user(tenant.getId(), USER_ROLE);
        User admin = user(tenant.getId(), ADMIN_ROLE);
        String ordinaryToken = login(ordinary);
        String adminToken = login(admin);
        AddDataSourceReqVo request = sourceRequest(tenant);
        mvc.perform(post("/api/dataSource/addDataSource").contentType("application/json")
                        .header("Authorization", "Bearer " + ordinaryToken).content(mapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/dataSource/addDataSource").contentType("application/json")
                        .header("Authorization", "Bearer " + adminToken).content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/unknown").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesTokensAndInvalidatesOldRefreshToken() throws Exception {
        Tenant tenant = tenant();
        register(tenant, "刷新租户");
        User user = user(tenant.getId(), USER_ROLE);
        var pair = loginPair(user);
        String accessToken = pair.path("accessToken").asText();
        String refreshToken = pair.path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 用 refresh token 换取新的双 token
        String refreshed = mvc.perform(post("/api/refresh").contentType("application/json")
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var newPair = mapper.readTree(refreshed).path("result");
        String newAccess = newPair.path("accessToken").asText();
        String newRefresh = newPair.path("refreshToken").asText();
        assertThat(newAccess).isNotBlank().isNotEqualTo(accessToken);
        assertThat(newRefresh).isNotBlank().isNotEqualTo(refreshToken);

        // rotation：旧 refresh token 立即作废
        mvc.perform(post("/api/refresh").contentType("application/json")
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        // 新 access token 可正常访问业务接口
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.name").value("刷新租户"));
    }

    @Test
    void logoutBlacklistsAccessTokenAndClearsRefreshTokens() throws Exception {
        Tenant tenant = tenant();
        register(tenant, "登出租户");
        User user = user(tenant.getId(), USER_ROLE);
        var pair = loginPair(user);
        String accessToken = pair.path("accessToken").asText();
        String refreshToken = pair.path("refreshToken").asText();

        // 登出前 access token 可用
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 登出：拉黑当前 access token 并清空 refresh token
        mvc.perform(post("/api/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 登出后同一 access token 被拉黑，拒绝访问
        mvc.perform(get("/api/test/getTestById").param("id", "1").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // refresh token 已被清空，无法再刷新
        mvc.perform(post("/api/refresh").contentType("application/json")
                        .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantName("tenant-" + UUID.randomUUID());
        tenant.setPhone(UUID.randomUUID().toString());
        return tenants.addTenant(tenant);
    }

    private User user(Integer tenantId, String authority) {
        Role role = ensureRole(authority);
        User user = new User();
        user.setUsername("u-" + UUID.randomUUID());
        user.setPassword(encoder.encode(PASSWORD));
        user.setTenantId(tenantId);
        user.setRoleId(role.getId());
        user.setStatus(1);
        return users.saveAndFlush(user);
    }

    private String login(User user) throws Exception {
        String token = loginPair(user).path("accessToken").asText();
        assertThat(jwtUtil.parseToken(token).getSubject()).isEqualTo(user.getUsername());
        return token;
    }

    private com.fasterxml.jackson.databind.JsonNode loginPair(User user) throws Exception {
        String json = mvc.perform(post("/api/login").contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginUser().setUsername(user.getUsername()).setPassword(PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("result");
    }

    private Role ensureRole(String roleName) {
        java.util.List<AuthNode> nodes = ensureAuthNodes();
        Role role = roles.findByRoleNameAndIsDelFalse(roleName).orElseGet(() -> {
            Role created = new Role();
            created.setRoleName(roleName);
            created.setParentId(0);
            created.setIsDel(false);
            return created;
        });
        java.math.BigInteger rights = java.math.BigInteger.ZERO;
        for (AuthNode node : nodes) {
            // 管理员拥有全部节点；普通用户仅拥有查询测试数据节点
            if (ADMIN_ROLE.equals(roleName) || "/api/test/**".equals(node.getPath())) {
                rights = rights.setBit(node.getId());
            }
        }
        role.setNodeRights(rights.toString());
        return roles.saveAndFlush(role);
    }

    private java.util.List<AuthNode> ensureAuthNodes() {
        var existing = authNodes.findAllByIsDelFalseOrderByIdAsc();
        if (existing.isEmpty()) {
            authNodes.saveAndFlush(node("查询租户测试数据", "/api/test/**", null));
            authNodes.saveAndFlush(node("新增租户账号", "/api/addTenantInfo", "POST"));
            authNodes.saveAndFlush(node("管理租户数据源", "/api/dataSource/**", null));
            existing = authNodes.findAllByIsDelFalseOrderByIdAsc();
        }
        authNodeService.refreshActiveAuthNodes();
        return existing;
    }

    private AuthNode node(String name, String path, String method) {
        AuthNode node = new AuthNode();
        node.setParentId(0);
        node.setName(name);
        node.setPath(path);
        node.setMethod(method);
        node.setListOrder(1);
        node.setIsDel(false);
        return node;
    }

    private AddDataSourceReqVo sourceRequest(Tenant tenant) {
        AddDataSourceReqVo request = new AddDataSourceReqVo();
        request.setTenantId(tenant.getId());
        request.setUrl("jdbc:h2:mem:tenant_" + tenant.getId() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        request.setUsername("sa");
        request.setPassword("test-only");
        return request;
    }

    private void register(Tenant tenant, String name) {
        sources.addTenantDataInfo(sourceRequest(tenant));
        JdbcTemplate jdbc = new JdbcTemplate(router().getResolvedDataSources().get(tenant.getId()));
        jdbc.execute("create table t_test (id int primary key, name varchar(255))");
        jdbc.update("insert into t_test(id, name) values(1, ?)", name);
    }

    private DynamicDataSource router() {
        return (DynamicDataSource) dataSource;
    }
}
