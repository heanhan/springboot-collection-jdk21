package com.example.dynamic.jpa.system.config.cluster;

import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.TenantDataSourceProperties;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.sql.DataSource;

/**
 * 多实例路由变更广播的 Redis 监听容器配置。
 *
 * <p>仅当 {@code app.datasource.cluster-sync.enabled=true} 时装配；关闭时不创建监听容器，
 * 不会主动连接 Redis，dev/test 与单机部署零影响。</p>
 *
 * @author zhaojh
 */
@Configuration
@ConditionalOnProperty(name = "app.datasource.cluster-sync.enabled", havingValue = "true")
public class RedisClusterSyncConfig {

    @Bean
    public DataSourceChangeListener dataSourceChangeListener(
            @Qualifier("multipleDataSource") DataSource router,
            TenantDataInfoService tenantDataInfoService) {
        return new DataSourceChangeListener((DynamicDataSource) router, tenantDataInfoService);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            DataSourceChangeListener dataSourceChangeListener,
            TenantDataSourceProperties properties) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(dataSourceChangeListener,
                new ChannelTopic(properties.getClusterSync().getChannel()));
        return container;
    }
}
