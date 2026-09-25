package com.example.dynamic.jpa.system.config.cluster;

import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * 租户数据源路由变更监听器：接收其它实例广播的变更，热更新本实例路由表。
 *
 * <p>ADD → 若本地未注册则懒加载该租户连接池；REMOVE → 摘除并关闭本地连接池。
 * 处理天然幂等，源实例重复处理自身广播不会造成副作用。</p>
 *
 * @author zhaojh
 */
@Slf4j
public class DataSourceChangeListener implements MessageListener {

    private final DynamicDataSource router;
    private final TenantDataInfoService tenantDataInfoService;

    public DataSourceChangeListener(DynamicDataSource router, TenantDataInfoService tenantDataInfoService) {
        this.router = router;
        this.tenantDataInfoService = tenantDataInfoService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        DataSourceChangeEvent event = DataSourceChangeEvent.parse(payload);
        if (event == null || event.tenantId() == null) {
            log.warn("[集群同步] 无法解析广播消息: {}", payload);
            return;
        }
        try {
            switch (event.type()) {
                case ADD -> {
                    if (!router.getResolvedDataSources().containsKey(event.tenantId())) {
                        tenantDataInfoService.ensureTenantDataSource(event.tenantId());
                        log.info("[集群同步] 收到 ADD，已加载租户数据源 {}", event.tenantId());
                    }
                }
                case REMOVE -> {
                    if (router.removeDataSource(event.tenantId())) {
                        log.info("[集群同步] 收到 REMOVE，已摘除租户数据源 {}", event.tenantId());
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("[集群同步] 处理路由变更失败 payload={}: {}", payload, e.getMessage(), e);
        }
    }
}
