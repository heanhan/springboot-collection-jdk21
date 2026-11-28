package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.enums.UserStatus;
import com.example.dynamic.jpa.common.util.RightsUtils;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.config.TenantDatabaseProvisioner;
import com.example.dynamic.jpa.system.config.TenantPasswordCipher;
import com.example.dynamic.jpa.system.dao.ProvisioningTaskDao;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.dao.UserRoleDao;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.ProvisioningTask;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.MenuService;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.service.TenantLifecycleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import com.example.dynamic.jpa.system.vo.CreateTenantReq;
import com.example.dynamic.jpa.system.vo.DestroyTenantReq;
import com.example.dynamic.jpa.system.vo.RetryProvisionReq;
import jakarta.annotation.Resource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantLifecycleServiceImpl implements TenantLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(TenantLifecycleServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final List<String> TENANT_ADMIN_PERMISSIONS = List.of(
            "PROFILE:READ", "PROFILE:UPDATE", "TENANT:READ", "TENANT:UPDATE", "TENANT:RENEW",
            "DATASOURCE:READ", "DATASOURCE:TEST", "USER:READ", "USER:CREATE", "USER:UPDATE",
            "USER:DISABLE", "USER:RESET-PASSWORD", "USER:ASSIGN-ROLE", "ROLE:READ",
            "ROLE:CREATE", "ROLE:UPDATE", "ROLE:ASSIGN-PERMISSION", "ROLE:DELETE",
            "TENANT-BUSINESS:READ", "TENANT-BUSINESS:WRITE");
    private static final List<String> TENANT_MEMBER_PERMISSIONS = List.of(
            "PROFILE:READ", "PROFILE:UPDATE", "TENANT:READ", "TENANT-BUSINESS:READ", "TENANT-BUSINESS:WRITE");

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantDao tenantDao;

    @Resource
    private TenantDataInfoService tenantDataInfoService;

    @Resource
    private UserService userService;

    @Resource
    private UserDao userDao;

    @Resource
    private UserRoleDao userRoleDao;

    @Resource
    private RoleService roleService;

    @Resource
    private RoleDao roleDao;

    @Resource
    private MenuService menuService;

    @Resource
    private ProvisioningTaskDao provisioningTaskDao;

    @Resource
    private TenantDatabaseProvisioner provisioner;

    @Resource
    private TenantPasswordCipher passwordCipher;

    private final Map<Integer, Object> tenantLocks = new ConcurrentHashMap<>();
    private final TransactionTemplate transactions;

    public TenantLifecycleServiceImpl(
            @Qualifier("systemTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public Tenant createTenant(CreateTenantReq request) {
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BaseException("到期时间不能早于当前时间");
        }
        Tenant tenant = new Tenant();
        tenant.setTenantName(request.getTenantName());
        tenant.setTenantCode(request.getTenantCode());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setExpiresAt(request.getExpiresAt());
        Tenant saved = tenantService.addTenant(tenant);
        Tenant result = withLock(saved.getId(), () -> provision(saved, request, null));
        if (result == null) {
            throw new BaseException("租户开通失败，请查看开通任务后重试");
        }
        return result;
    }

    @Override
    public Tenant retryProvision(Integer tenantId, RetryProvisionReq request) {
        return withLock(tenantId, () -> {
            Tenant tenant = requireTenant(tenantId);
            if ("DESTROYED".equals(tenant.getStatus())
                    || ("ACTIVE".equals(tenant.getStatus()) && "SUCCESS".equals(tenant.getProvisionStatus()))) {
                throw new BaseException("当前租户无需重试开通");
            }
            Tenant result = provision(tenant, null, request);
            if (result == null) {
                throw new BaseException("重试开通失败，请查看开通任务");
            }
            return result;
        });
    }

    private Tenant provision(Tenant tenant, CreateTenantReq initialRequest, RetryProvisionReq retryRequest) {
        ProvisioningTask task = startTask(tenant.getId(), initialRequest == null ? "RETRY" : "CREATE");
        String step = "VALIDATE";
        String databaseName = tenant.getDatabaseName();
        String runtimeUsername = null;
        try {
            step = "CREATE_DATABASE";
            if (databaseName == null || databaseName.isBlank()) {
                databaseName = "dt_tenant_" + tenant.getId() + "_" + randomSuffix();
            }
            runtimeUsername = "dt_" + tenant.getId() + "_" + randomSuffix();
            String runtimePassword = randomPassword();
            tenant.setDatabaseName(databaseName);
            tenant.setProvisionStatus("RUNNING");
            tenant.setProvisionError(null);
            saveTenant(tenant);
            try (Connection connection = provisioner.connect()) {
                provisioner.createDatabase(connection, databaseName);
                step = "CREATE_USER";
                provisioner.createRuntimeUser(connection, runtimeUsername, runtimePassword, databaseName);
            }
            step = "SAVE_DATASOURCE";
            TenantData data = tenantDataInfoService.getTenantData(tenant.getId()).orElseGet(TenantData::new);
            data.setTenantId(tenant.getId());
            data.setUrl(provisioner.databaseUrl(databaseName));
            data.setUsername(runtimeUsername);
            data.setPassword(passwordCipher.encrypt(runtimePassword));
            data.setPasswordCipherVersion(passwordCipher.isEnabled() ? 1 : 0);
            data.setStatus("PROVISIONING");
            data.setSchemaVersion(0);
            data.setIsDel(false);
            if (data.getCreateTime() == null) {
                data.setCreateTime(LocalDateTime.now());
            }
            data.setUpdateTime(LocalDateTime.now());
            tenantDataInfoService.saveTenantData(data);
            step = "MIGRATE_SCHEMA";
            DriverManagerDataSource migrationDataSource = new DriverManagerDataSource(
                    provisioner.databaseUrl(databaseName), runtimeUsername, runtimePassword);
            Flyway flyway = Flyway.configure()
                    .dataSource(migrationDataSource)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .cleanDisabled(true)
                    .load();
            flyway.migrate();
            int schemaVersion = flyway.info().current() == null ? 0
                    : Integer.parseInt(flyway.info().current().getVersion().getVersion().replaceFirst("^V", ""));
            step = "INITIALIZE_ROLES";
            Role adminRole = ensureRole(tenant.getId(), SystemConstant.ROLE_TENANT_ADMIN,
                    "租户管理员", TENANT_ADMIN_PERMISSIONS);
            ensureRole(tenant.getId(), "ROLE_TENANT_MEMBER", "租户成员", TENANT_MEMBER_PERMISSIONS);
            step = "INITIALIZE_ADMIN";
            if (initialRequest != null) {
                createAdministrator(tenant, adminRole, initialRequest);
            } else {
                restoreOrCreateAdministrator(tenant, adminRole, retryRequest);
            }
            step = "ACTIVATE_DATASOURCE";
            data.setStatus("READY");
            data.setSchemaVersion(schemaVersion);
            data.setIsDel(false);
            tenantDataInfoService.saveTenantData(data);
            tenant.setSchemaVersion(schemaVersion);
            tenant.setStatus("ACTIVE");
            tenant.setProvisionStatus("SUCCESS");
            tenant.setProvisionError(null);
            saveTenant(tenant);
            tenantDataInfoService.activateTenantDataSource(tenant.getId());
            finishTask(task.getId(), "SUCCESS", "COMPLETED", null);
            return tenantDao.findById(tenant.getId()).orElse(tenant);
        } catch (Exception e) {
            SQLException sqlException = findSqlException(e);
            Throwable rootCause = rootCause(e);
            log.error("租户开通失败 tenantId={} step={} errorType={} rootType={} sqlState={} errorCode={}",
                    tenant.getId(), step, e.getClass().getSimpleName(), rootCause.getClass().getName(),
                    sqlException == null ? null : sqlException.getSQLState(),
                    sqlException == null ? null : sqlException.getErrorCode());
            compensate(tenant, databaseName, runtimeUsername);
            markProvisionFailure(tenant, "开通失败：" + step + "，请检查开通账号权限并重试");
            finishTask(task.getId(), "FAILED", step, "开通失败：" + step);
            return null;
        }
    }

    private Role ensureRole(Integer tenantId, String roleCode, String roleName, List<String> permissions) {
        Role role = roleDao.findByRoleCodeAndTenantIdAndIsDelFalse(roleCode, tenantId).orElse(null);
        if (role == null) {
            role = new Role();
            role.setTenantId(tenantId);
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setDescription(roleName);
            role.setMenuRights(allMenuRights());
            role.setType(1);
            roleService.addRole(role);
            role = roleDao.findByRoleCodeAndTenantIdAndIsDelFalse(roleCode, tenantId).orElseThrow();
        }
        roleService.replaceRolePermissions(role.getId(), permissions);
        return role;
    }

    private String allMenuRights() {
        BigInteger rights = BigInteger.ZERO;
        for (Menu menu : menuService.listAll()) {
            rights = rights.setBit(menu.getId());
        }
        return rights.toString();
    }

    private void createAdministrator(Tenant tenant, Role role, CreateTenantReq request) {
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setUsername(request.getAdminUsername());
        user.setPassword(request.getAdminPassword());
        user.setNickname(request.getAdminUsername());
        user.setPhone(request.getAdminPhone());
        user.setEmail(request.getAdminEmail());
        user.setRoleId(role.getId());
        user.setType(1);
        userService.addUser(user);
    }

    private void restoreOrCreateAdministrator(Tenant tenant, Role role, RetryProvisionReq request) {
        User user = userRoleDao.findByRoleId(role.getId()).stream()
                .map(relation -> userDao.findById(relation.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        if (user != null) {
            if (!Objects.equals(user.getTenantId(), tenant.getId())) {
                throw new BaseException("租户管理员归属异常");
            }
            user.setStatus(UserStatus.NORMAL.getValue());
            user.setRoleId(role.getId());
            userDao.save(user);
            userService.assignRoles(user.getId(), List.of(role.getId()));
            return;
        }
        if (request == null || request.getAdminUsername() == null || request.getAdminUsername().isBlank()
                || request.getAdminPassword() == null || request.getAdminPassword().isBlank()
                || request.getAdminPhone() == null || request.getAdminPhone().isBlank()) {
            throw new BaseException("未找到租户管理员，请补充管理员账号、密码和手机号后重试");
        }
        CreateTenantReq create = new CreateTenantReq();
        create.setAdminUsername(request.getAdminUsername());
        create.setAdminPassword(request.getAdminPassword());
        create.setAdminPhone(request.getAdminPhone());
        create.setAdminEmail(request.getAdminEmail());
        createAdministrator(tenant, role, create);
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private SQLException findSqlException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private void compensate(Tenant tenant, String databaseName, String runtimeUsername) {
        tenantDataInfoService.disableTenantDataSource(tenant.getId());
        if (databaseName == null || runtimeUsername == null) {
            return;
        }
        try (Connection connection = provisioner.connect()) {
            provisioner.dropDatabaseAndUser(connection, databaseName, runtimeUsername);
        } catch (Exception e) {
            log.error("租户开通补偿清理失败 tenantId={} errorType={}", tenant.getId(), e.getClass().getSimpleName());
        }
    }

    @Override
    public Tenant renew(Integer tenantId, int days) {
        return withLock(tenantId, () -> {
            if (days < 1 || days > 3650) {
                throw new BaseException("续期天数范围为1-3650天");
            }
            Tenant tenant = requireTenant(tenantId);
            if (!"ACTIVE".equals(tenant.getStatus()) && !"DISABLED".equals(tenant.getStatus())) {
                throw new BaseException("当前状态不支持续期");
            }
            LocalDateTime base = tenant.getExpiresAt() != null && tenant.getExpiresAt().isAfter(LocalDateTime.now())
                    ? tenant.getExpiresAt() : LocalDateTime.now();
            tenant.setExpiresAt(base.plusDays(days));
            tenant.setUpdateTime(LocalDateTime.now());
            return saveTenant(tenant);
        });
    }

    @Override
    public Tenant disable(Integer tenantId) {
        return withLock(tenantId, () -> {
            Tenant tenant = requireTenant(tenantId);
            if ("DISABLED".equals(tenant.getStatus())) {
                return tenant;
            }
            if (!"ACTIVE".equals(tenant.getStatus())) {
                throw new BaseException("仅已开通租户可以停用");
            }
            tenant.setStatus("DISABLED");
            tenant.setUpdateTime(LocalDateTime.now());
            saveTenant(tenant);
            userService.revokeTenantSessions(tenantId);
            tenantDataInfoService.disableTenantDataSource(tenantId);
            return tenant;
        });
    }

    @Override
    public Tenant enable(Integer tenantId) {
        return withLock(tenantId, () -> {
            Tenant tenant = requireTenant(tenantId);
            if (!"DISABLED".equals(tenant.getStatus())) {
                throw new BaseException("仅已停用租户可以恢复");
            }
            if (tenant.getExpiresAt() != null && tenant.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BaseException("租户已到期，请先续期");
            }
            TenantData data = tenantDataInfoService.getTenantData(tenantId)
                    .filter(item -> "READY".equals(item.getStatus()) || "DISABLED".equals(item.getStatus()))
                    .orElseThrow(() -> new BaseException("租户数据源尚未就绪"));
            data.setStatus("READY");
            data.setIsDel(false);
            data.setUpdateTime(LocalDateTime.now());
            tenantDataInfoService.saveTenantData(data);
            tenant.setStatus("ACTIVE");
            tenant.setUpdateTime(LocalDateTime.now());
            saveTenant(tenant);
            try {
                tenantDataInfoService.activateTenantDataSource(tenantId);
            } catch (RuntimeException e) {
                disable(tenantId);
                throw e;
            }
            return tenant;
        });
    }

    @Override
    public Tenant destroy(Integer tenantId, DestroyTenantReq request) {
        return withLock(tenantId, () -> {
            Tenant tenant = requireTenant(tenantId);
            if ("DESTROYED".equals(tenant.getStatus())) {
                return tenant;
            }
            if (!Objects.equals(tenant.getTenantCode(), request.getTenantCode())) {
                throw new BaseException("租户编码确认不一致");
            }
            ProvisioningTask task = startTask(tenantId, "DESTROY");
            tenant.setStatus("DISABLED");
            tenant.setUpdateTime(LocalDateTime.now());
            saveTenant(tenant);
            userService.revokeTenantSessions(tenantId);
            tenantDataInfoService.disableTenantDataSource(tenantId);
            TenantData data = tenantDataInfoService.getTenantData(tenantId).orElse(null);
            try {
                if (tenant.getDatabaseName() != null && !tenant.getDatabaseName().isBlank()) {
                    try (Connection connection = provisioner.connect()) {
                        provisioner.dropDatabaseAndUser(connection, tenant.getDatabaseName(),
                                data == null ? null : data.getUsername());
                    }
                }
                userService.destroyTenantUsers(tenantId);
                transactions.executeWithoutResult(status -> {
                    Tenant current = tenantDao.findById(tenantId).orElseThrow();
                    current.setStatus("DESTROYED");
                    current.setDestroyedAt(LocalDateTime.now());
                    current.setUpdateTime(LocalDateTime.now());
                    tenantDao.save(current);
                    roleDao.findAllByTenantIdAndIsDelFalseOrderByIdAsc(tenantId).forEach(role -> {
                        role.setIsDel(true);
                        role.setUpdateTime(LocalDateTime.now());
                        roleDao.save(role);
                    });
                });
                finishTask(task.getId(), "SUCCESS", "COMPLETED", null);
                return tenantDao.findById(tenantId).orElseThrow();
            } catch (Exception e) {
                log.error("租户销毁失败 tenantId={} errorType={}", tenantId, e.getClass().getSimpleName());
                tenant.setProvisionStatus("FAILED");
                tenant.setProvisionError("销毁失败，租户已保持停用，请检查开通账号权限后重试");
                saveTenant(tenant);
                finishTask(task.getId(), "FAILED", "DROP_DATABASE", "销毁失败，租户保持停用");
                throw new BaseException("销毁失败，租户已保持停用，请检查数据库开通账号权限");
            }
        });
    }

    @Override
    public List<ProvisioningTask> listTasks(Integer tenantId) {
        return provisioningTaskDao.findTop10ByTenantIdOrderByIdDesc(tenantId);
    }

    private ProvisioningTask startTask(Integer tenantId, String type) {
        return transactions.execute(status -> {
            ProvisioningTask task = new ProvisioningTask();
            task.setTenantId(tenantId);
            task.setTaskType(type);
            task.setStatus("RUNNING");
            task.setCurrentStep("STARTED");
            task.setStartTime(LocalDateTime.now());
            return provisioningTaskDao.saveAndFlush(task);
        });
    }

    private void finishTask(Integer taskId, String status, String step, String error) {
        transactions.executeWithoutResult(txStatus -> {
            ProvisioningTask task = provisioningTaskDao.findById(taskId).orElseThrow();
            task.setStatus(status);
            task.setCurrentStep(step);
            task.setErrorMessage(error);
            task.setFinishTime(LocalDateTime.now());
            provisioningTaskDao.save(task);
        });
    }

    private void markProvisionFailure(Tenant tenant, String message) {
        transactions.executeWithoutResult(status -> {
            Tenant current = tenantDao.findById(tenant.getId()).orElseThrow();
            current.setProvisionStatus("FAILED");
            current.setProvisionError(message);
            current.setUpdateTime(LocalDateTime.now());
            tenantDao.save(current);
        });
    }

    private Tenant saveTenant(Tenant tenant) {
        return transactions.execute(status -> tenantDao.saveAndFlush(tenant));
    }

    private Tenant requireTenant(Integer tenantId) {
        Tenant tenant = tenantId == null ? null : tenantDao.findByIdAndIsDelFalse(tenantId).orElse(null);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        return tenant;
    }

    private <T> T withLock(Integer tenantId, java.util.function.Supplier<T> action) {
        Object lock = tenantLocks.computeIfAbsent(tenantId, ignored -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }

    private String randomSuffix() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private String randomPassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
