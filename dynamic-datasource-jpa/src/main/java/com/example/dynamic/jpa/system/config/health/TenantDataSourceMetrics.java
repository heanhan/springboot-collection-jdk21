package com.example.dynamic.jpa.system.config.health;

import com.example.dynamic.jpa.system.config.DynamicDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 多租户数据源指标：向 Micrometer 注册"已注册租户数据源数量"仪表盘，便于容量监控与告警。
 *
 * @author zhaojh
 */
@Component
public class TenantDataSourceMetrics {

    private static final Object SYSTEM_KEY = Integer.valueOf(0);

    public TenantDataSourceMetrics(MeterRegistry registry, @Qualifier("multipleDataSource") DataSource router) {
        DynamicDataSource dynamicDataSource = (DynamicDataSource) router;
        Gauge.builder("dynjpa.tenant.datasources", dynamicDataSource, TenantDataSourceMetrics::countTenants)
                .description("已注册的租户数据源数量（不含系统库）")
                .register(registry);
    }

    private static double countTenants(DynamicDataSource router) {
        try {
            return router.getResolvedDataSources().keySet().stream()
                    .filter(key -> !SYSTEM_KEY.equals(key))
                    .count();
        } catch (RuntimeException e) {
            return 0d;
        }
    }
}
