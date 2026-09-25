package com.example.dynamic.jpa.system.config.health;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 多租户数据源健康检查：暴露系统库注册状态、已注册租户数与各租户 Druid 连接池活跃度。
 *
 * <p>仅读取内存路由表与连接池计数，不主动建连，避免健康检查对租户库造成额外负载。</p>
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class TenantDataSourceHealthIndicator implements HealthIndicator {

    private static final Object SYSTEM_KEY = Integer.valueOf(0);

    private final DynamicDataSource router;

    public TenantDataSourceHealthIndicator(@Qualifier("multipleDataSource") DataSource router) {
        this.router = (DynamicDataSource) router;
    }

    @Override
    public Health health() {
        try {
            Map<Object, DataSource> resolved = router.getResolvedDataSources();
            boolean systemUp = resolved.containsKey(SYSTEM_KEY);
            Health.Builder builder = systemUp ? Health.up() : Health.down();
            builder.withDetail("systemDataSource", systemUp ? "registered" : "missing");
            int tenantCount = 0;
            for (Map.Entry<Object, DataSource> entry : resolved.entrySet()) {
                if (SYSTEM_KEY.equals(entry.getKey())) {
                    continue;
                }
                tenantCount++;
                if (entry.getValue() instanceof DruidDataSource druid) {
                    builder.withDetail("tenant_" + entry.getKey() + "_active", druid.getActiveCount());
                    builder.withDetail("tenant_" + entry.getKey() + "_pooling", druid.getPoolingCount());
                }
            }
            builder.withDetail("tenantDataSourceCount", tenantCount);
            return builder.build();
        } catch (RuntimeException e) {
            log.warn("租户数据源健康检查失败: {}", e.getMessage());
            return Health.down(e).build();
        }
    }
}
