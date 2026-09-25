package com.example.dynamic.jpa.system.config;

import com.example.dynamic.jpa.system.vo.LoginInfo;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.IntFunction;
import jakarta.annotation.PreDestroy;
import com.alibaba.druid.pool.DruidDataSource;
import java.util.Map;

/**
 * @Author: zhaojh
 * @ClassName: DynamicDataSource
 * @Description: 动态切换数据源
 */
public class DynamicDataSource extends AbstractRoutingDataSource implements InitializingBean , ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);

    private ApplicationContext applicationContext ;


    @Nullable
    private Map<Object, Object> targetDataSources;

    @Nullable
    private Object defaultTargetDataSource;

    private boolean lenientFallback = false;

    private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

    @Nullable
    private volatile Map<Object, DataSource> resolvedDataSources;

    @Nullable
    private DataSource resolvedDefaultDataSource;

    /**
     * 运行期租户数据源懒加载器：入参 tenantId，返回已注册的连接池；未知租户返回 null。
     * 由启动初始化逻辑注入，未注入时未命中键直接报错。
     */
    @Nullable
    private volatile IntFunction<DataSource> lazyLoader;

    /**
     * 运行期严格模式标记：启动完成后置 true，此时缺失租户上下文回退系统库会打印告警（不再静默）。
     */
    private volatile boolean runtimeStrict = false;

    /**
     * 注入运行期懒加载器。
     */
    public void setLazyLoader(@Nullable IntFunction<DataSource> lazyLoader) {
        this.lazyLoader = lazyLoader;
    }

    /**
     * 标记启动完成，开启运行期缺失上下文告警。
     */
    public void markRuntimeReady() {
        this.runtimeStrict = true;
    }

    public void setTargetDataSources(@NonNull Map<Object, Object> targetDataSources) {
        this.targetDataSources = Map.copyOf(targetDataSources);
    }

    /**
     * 直接加入数据源
     *
     * @param key   数据源的key
     * @param value 数据源
     */
    public synchronized void addDataSources(@NonNull Object key, @NonNull DataSource value) {
        Assert.state(this.resolvedDataSources != null, "数据源路由尚未初始化");
        Object lookupKey = resolveSpecifiedLookupKey(key);
        Assert.isTrue(!this.resolvedDataSources.containsKey(lookupKey), "租户数据源已存在");
        Map<Object, DataSource> updated = new HashMap<>(this.resolvedDataSources);
        updated.put(lookupKey, value);
        this.resolvedDataSources = Map.copyOf(updated);
    }

    /**
     * 移除并关闭租户数据源，系统数据源不允许移除
     */
    public synchronized boolean removeDataSource(@NonNull Object key) {
        Assert.state(this.resolvedDataSources != null, "数据源路由尚未初始化");
        Object lookupKey = resolveSpecifiedLookupKey(key);
        Assert.isTrue(!Integer.valueOf(0).equals(lookupKey), "系统数据源不允许移除");
        DataSource removed = this.resolvedDataSources.get(lookupKey);
        if (removed == null) {
            return false;
        }
        Map<Object, DataSource> updated = new HashMap<>(this.resolvedDataSources);
        updated.remove(lookupKey);
        this.resolvedDataSources = Map.copyOf(updated);
        if (removed instanceof DruidDataSource druid) {
            druid.close();
        }
        return true;
    }

    public void setDefaultTargetDataSource(@NonNull Object defaultTargetDataSource) {
        this.defaultTargetDataSource = defaultTargetDataSource;
    }

    public void setLenientFallback(boolean lenientFallback) {
        this.lenientFallback = lenientFallback;
    }

    public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
        this.dataSourceLookup = dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup();
    }

    @Override
    public synchronized void afterPropertiesSet() {
        if (this.targetDataSources == null) {
            throw new IllegalArgumentException("Property 'targetDataSources' is required");
        } else {
            if (targetDataSources.isEmpty()) {
                return;
            }
            Map<Object, DataSource> resolved = new HashMap<>();
            this.targetDataSources.forEach((key, value) -> {
                Object lookupKey = this.resolveSpecifiedLookupKey(key);
                DataSource dataSource = this.resolveSpecifiedDataSource(value);
                resolved.put(lookupKey, dataSource);
            });
            this.resolvedDataSources = Map.copyOf(resolved);
            if (this.defaultTargetDataSource != null) {
                this.resolvedDefaultDataSource = this.resolveSpecifiedDataSource(this.defaultTargetDataSource);
            }

        }
    }

    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        Assert.isTrue(lookupKey instanceof Integer && (Integer) lookupKey >= 0,
                "数据源key必须是非负整数");
        return lookupKey;
    }

    protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
        if (dataSource instanceof DataSource) {
            return (DataSource) dataSource;
        } else if (dataSource instanceof String) {
            return this.dataSourceLookup.getDataSource((String) dataSource);
        } else {
            throw new IllegalArgumentException("Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
        }
    }

    public Map<Object, DataSource> getResolvedDataSources() {
        Assert.state(this.resolvedDataSources != null, "DataSources not resolved yet - call afterPropertiesSet");
        return Collections.unmodifiableMap(this.resolvedDataSources);
    }

    @Nullable
    public DataSource getResolvedDefaultDataSource() {
        return this.resolvedDefaultDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.determineTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.determineTargetDataSource().getConnection(username, password);
    }

    @Override
    @NotNull
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return iface.isInstance(this) ? (T) this : this.determineTargetDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || this.determineTargetDataSource().isWrapperFor(iface);
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
        Object lookupKey = this.determineCurrentLookupKey();
        DataSource dataSource = this.resolvedDataSources.get(lookupKey);
        // 未命中且已配置懒加载器：按需从系统库加载该租户连接池（启动失败隔离后的兜底路径）
        if (dataSource == null && this.lazyLoader != null && lookupKey instanceof Integer tenantId && tenantId > 0) {
            dataSource = loadLazily(tenantId);
        }
        if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
            dataSource = this.resolvedDefaultDataSource;
        }
        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
        } else {
            return dataSource;
        }
    }

    /**
     * 懒加载并注册租户连接池；并发下同一租户仅构建一次。
     */
    private DataSource loadLazily(int tenantId) {
        synchronized (this) {
            DataSource existing = this.resolvedDataSources.get(tenantId);
            if (existing != null) {
                return existing;
            }
            IntFunction<DataSource> loader = this.lazyLoader;
            if (loader == null) {
                return null;
            }
            return loader.apply(tenantId);
        }
    }

    protected Object determineCurrentLookupKey() {
        // 返回要使用的数据源的key
        LoginInfo tenant = LoginInfoHolder.getTenant();
        if (tenant == null) {
            // 缺失租户上下文：启动期/系统级操作默认走系统库；运行期打印告警以暴露异步/定时任务的上下文丢失
            if (runtimeStrict) {
                log.warn("租户上下文缺失，回退系统库(tenantId=0)。若为异步/定时任务，请显式设置租户上下文或使用 TTL 执行器");
            }
            return LoginInfoHolder.SYSTEM_TENANT_ID;
        }
        Assert.notNull(tenant.getTenantId(), "租户id不能为空");
        return tenant.getTenantId();
    }

    @PreDestroy
    public void closeTenantDataSources() {
        if (resolvedDataSources != null) {
            resolvedDataSources.forEach((key, source) -> {
                if (!Integer.valueOf(0).equals(key) && source instanceof DruidDataSource druid) {
                    druid.close();
                }
            });
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


}
