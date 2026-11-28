package com.example.dynamic.jpa.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

public class DynamicDataSource extends AbstractRoutingDataSource
        implements InitializingBean, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);

    private ApplicationContext applicationContext;
    private Map<Object, DataSource> resolvedDataSources;
    private volatile IntFunction<DataSource> lazyLoader;
    private final Map<Integer, Object> tenantLocks = new ConcurrentHashMap<>();
    private final Map<Object, Integer> poolCapacities = new HashMap<>();
    private final Map<Object, Long> lastUsedAt = new HashMap<>();
    private int maxTotalConnections = 200;

    @Override
    public synchronized void afterPropertiesSet() {
        this.resolvedDataSources = Collections.emptyMap();
    }

    public void setLazyLoader(IntFunction<DataSource> lazyLoader) {
        this.lazyLoader = lazyLoader;
    }

    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        Assert.notNull(targetDataSources, "targetDataSources不能为空");
    }

    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        Assert.isTrue(lookupKey instanceof Integer && (Integer) lookupKey > 0,
                "租户数据源key必须是正整数");
        return lookupKey;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = Math.max(1, maxTotalConnections);
    }

    public synchronized void addDataSources(Object key, DataSource value) {
        addDataSources(key, value, value instanceof DruidDataSource druid ? druid.getMaxActive() : 1);
    }

    public synchronized void addDataSources(Object key, DataSource value, int capacity) {
        Assert.state(resolvedDataSources != null, "数据源路由尚未初始化");
        Object lookupKey = resolveSpecifiedLookupKey(key);
        Assert.isTrue(!resolvedDataSources.containsKey(lookupKey), "租户数据源已存在");
        int used = poolCapacities.values().stream().mapToInt(Integer::intValue).sum();
        while (used + capacity > maxTotalConnections && !resolvedDataSources.isEmpty()) {
            Object oldest = lastUsedAt.entrySet().stream()
                    .filter(entry -> resolvedDataSources.containsKey(entry.getKey()))
                    .min(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey).orElse(null);
            if (oldest == null) {
                break;
            }
            DataSource removed = resolvedDataSources.get(oldest);
            used -= poolCapacities.getOrDefault(oldest, 0);
            poolCapacities.remove(oldest);
            lastUsedAt.remove(oldest);
            Map<Object, DataSource> updated = new HashMap<>(resolvedDataSources);
            updated.remove(oldest);
            resolvedDataSources = Collections.unmodifiableMap(updated);
            closePool(removed);
        }
        Assert.isTrue(used + capacity <= maxTotalConnections, "租户连接池超过全局连接预算");
        Map<Object, DataSource> updated = new HashMap<>(resolvedDataSources);
        updated.put(lookupKey, value);
        resolvedDataSources = Collections.unmodifiableMap(updated);
        poolCapacities.put(lookupKey, Math.max(1, capacity));
        lastUsedAt.put(lookupKey, System.currentTimeMillis());
    }

    public synchronized boolean removeDataSource(Object key) {
        Assert.state(resolvedDataSources != null, "数据源路由尚未初始化");
        Object lookupKey = resolveSpecifiedLookupKey(key);
        DataSource removed = resolvedDataSources.get(lookupKey);
        if (removed == null) {
            return false;
        }
        Map<Object, DataSource> updated = new HashMap<>(resolvedDataSources);
        updated.remove(lookupKey);
        resolvedDataSources = Collections.unmodifiableMap(updated);
        poolCapacities.remove(lookupKey);
        lastUsedAt.remove(lookupKey);
        closePool(removed);
        return true;
    }

    public Map<Object, DataSource> getResolvedDataSources() {
        Assert.state(resolvedDataSources != null, "DataSources not resolved yet");
        return resolvedDataSources;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return determineTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineTargetDataSource().getConnection(username, password);
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Assert.state(resolvedDataSources != null, "DataSource router not initialized");
        Object lookupKey = determineCurrentLookupKey();
        DataSource dataSource = resolvedDataSources.get(lookupKey);
        if (dataSource == null && lazyLoader != null && lookupKey instanceof Integer tenantId) {
            dataSource = loadLazily(tenantId);
        }
        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for tenant [" + lookupKey + "]");
        }
        synchronized (this) {
            lastUsedAt.put(lookupKey, System.currentTimeMillis());
        }
        return dataSource;
    }

    protected Object determineCurrentLookupKey() {
        var loginInfo = LoginInfoHolder.getTenant();
        if (loginInfo == null || loginInfo.getTenantId() == null || loginInfo.getTenantId() <= 0) {
            throw new IllegalStateException("租户上下文缺失，禁止访问租户数据源");
        }
        return loginInfo.getTenantId();
    }

    private DataSource loadLazily(int tenantId) {
        Object lock = tenantLocks.computeIfAbsent(tenantId, ignored -> new Object());
        synchronized (lock) {
            DataSource existing = resolvedDataSources.get(tenantId);
            if (existing != null) {
                return existing;
            }
            IntFunction<DataSource> loader = lazyLoader;
            return loader == null ? null : loader.apply(tenantId);
        }
    }

    private void closePool(DataSource dataSource) {
        if (dataSource instanceof DruidDataSource druid) {
            druid.close();
        }
    }

    @PreDestroy
    public void closeTenantDataSources() {
        if (resolvedDataSources != null) {
            resolvedDataSources.values().forEach(this::closePool);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
