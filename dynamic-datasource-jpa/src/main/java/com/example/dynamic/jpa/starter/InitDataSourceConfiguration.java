package com.example.dynamic.jpa.starter;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.config.TenantDataSourceFactory;
import com.example.dynamic.jpa.system.config.TenantDataSourceProperties;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 启动时注册租户数据源。
 *
 * <p>生产化改造：</p>
 * <ul>
 *   <li>失败隔离：单个租户库不可达仅告警跳过，不再中断整个应用启动（除非 fail-fast=true）；</li>
 *   <li>懒加载兜底：向路由注入 lazyLoader，运行期未命中的租户按需从系统库构建连接池；</li>
 *   <li>系统上下文：注册期对系统库的查询显式运行在 tenantId=0 上下文；</li>
 *   <li>启动完成后开启运行期缺失上下文告警。</li>
 * </ul>
 *
 * @author zhaojh
 */
@Component
@Slf4j
public class InitDataSourceConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private final TenantDataInfoService tenantDataInfoService;

    private final TenantDataSourceFactory tenantDataSourceFactory;

    private final TenantDataSourceProperties properties;

    @Resource(name = "multipleDataSource")
    private DataSource dataSource;

    public InitDataSourceConfiguration(TenantDataInfoService tenantDataInfoService,
                                       TenantDataSourceFactory tenantDataSourceFactory,
                                       TenantDataSourceProperties properties) {
        this.tenantDataInfoService = tenantDataInfoService;
        this.tenantDataSourceFactory = tenantDataSourceFactory;
        this.properties = properties;
    }

    @Override
    public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        DynamicDataSource router = (DynamicDataSource) dataSource;
        // 注入运行期懒加载器：未命中租户键时按需构建（启动失败隔离后的兜底路径）
        router.setLazyLoader(tenantDataInfoService::ensureTenantDataSource);

        int loaded = 0;
        int failed = 0;
        if (properties.getStartup().isEagerRegister()) {
            List<TenantData> tenants = LoginInfoHolder.callAsSystem(tenantDataInfoService::listActiveTenantDataInfo);
            for (TenantData tenant : tenants) {
                Integer tenantId = tenant.getTenantId();
                if (tenantId == null || tenantId <= 0) {
                    failed++;
                    log.error("租户数据源id非法，已跳过: {}", tenantId);
                    if (properties.getStartup().isFailFast()) {
                        throw new IllegalStateException("租户数据源id必须大于0");
                    }
                    continue;
                }
                if (router.getResolvedDataSources().containsKey(tenantId)) {
                    loaded++;
                    continue;
                }
                DruidDataSource pool = tenantDataSourceFactory.create(tenant);
                try {
                    try (java.sql.Connection connection = pool.getConnection()) {
                        connection.getMetaData();
                    }
                    router.addDataSources(tenantId, pool);
                    loaded++;
                } catch (Exception e) {
                    pool.close();
                    failed++;
                    log.error("初始化租户数据源失败，已隔离跳过（运行期将懒加载重试）tenantId={}: {}", tenantId, e.getMessage());
                    if (properties.getStartup().isFailFast()) {
                        throw new IllegalStateException("初始化租户数据源失败，tenantId=" + tenantId, e);
                    }
                }
            }
        }
        // 启动完成，开启运行期缺失租户上下文告警
        router.markRuntimeReady();
        log.info("租户数据源初始化完成：成功 {} 个，失败 {} 个（懒加载兜底）", loaded, failed);
    }
}
