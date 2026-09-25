package com.example.dynamic.jpa.system.config.cluster;

import com.example.dynamic.jpa.system.config.TenantDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 租户数据源路由变更广播发布器。
 *
 * <p>仅在 {@code app.datasource.cluster-sync.enabled=true} 时真正发送；Redis 不可达时降级为告警，
 * 不影响本实例的路由变更（本实例已在事务提交后直接更新路由表）。</p>
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class DataSourceChangePublisher {

    private final StringRedisTemplate redis;
    private final TenantDataSourceProperties.ClusterSync config;

    public DataSourceChangePublisher(StringRedisTemplate redis, TenantDataSourceProperties properties) {
        this.redis = redis;
        this.config = properties.getClusterSync();
    }

    public void publish(DataSourceChangeEvent event) {
        if (!config.isEnabled() || event == null) {
            return;
        }
        try {
            redis.convertAndSend(config.getChannel(), event.serialize());
            log.info("[集群同步] 已广播租户数据源变更 {}", event.serialize());
        } catch (RuntimeException e) {
            log.warn("[集群同步降级] 广播失败 event={}: {}", event.serialize(), e.getMessage());
        }
    }
}
