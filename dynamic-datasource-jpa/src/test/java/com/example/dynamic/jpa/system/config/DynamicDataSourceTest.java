package com.example.dynamic.jpa.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicDataSourceTest {
    @AfterEach
    void clear() {
        LoginInfoHolder.clear();
    }

    @Test
    void routesIntegerTenantIdsAndFailsClosedForUnknownOrNullIds() throws Exception {
        DataSource system = mock(DataSource.class);
        DataSource tenant = mock(DataSource.class);
        Connection systemConnection = mock(Connection.class);
        Connection tenantConnection = mock(Connection.class);
        when(system.getConnection()).thenReturn(systemConnection);
        when(tenant.getConnection()).thenReturn(tenantConnection);
        DynamicDataSource router = router(system);
        router.addDataSources(1, tenant);
        assertThat(router.getConnection()).isSameAs(systemConnection);
        LoginInfo info = new LoginInfo();
        info.setTenantId(1);
        LoginInfoHolder.setTenant(info);
        assertThat(router.getConnection()).isSameAs(tenantConnection);
        info.setTenantId(999);
        assertThatThrownBy(router::getConnection).isInstanceOf(IllegalStateException.class);
        info.setTenantId(null);
        assertThatThrownBy(router::getConnection).isInstanceOf(IllegalArgumentException.class);
        verify(system, times(1)).getConnection();
        assertThatThrownBy(() -> router.addDataSources("1", tenant)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.addDataSources(1, tenant)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentRegistrationsPublishImmutableSnapshotsWithoutLosingTenants() throws Exception {
        DataSource system = mock(DataSource.class);
        DynamicDataSource router = router(system);
        Map<Object, DataSource> originalSnapshot = router.getResolvedDataSources();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int id = 1; id <= 40; id++) {
                int tenantId = id;
                tasks.add(() -> {
                    router.addDataSources(tenantId, system);
                    assertThat(router.getResolvedDataSources()).containsKeys(0, tenantId);
                    return null;
                });
            }
            for (var future : executor.invokeAll(tasks)) {
                future.get();
            }
        }
        assertThat(originalSnapshot).hasSize(1);
        assertThat(router.getResolvedDataSources()).hasSize(41);
        assertThatThrownBy(() -> router.getResolvedDataSources().put(99, system))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void closesTenantPoolsOnShutdown() {
        DruidDataSource system = mock(DruidDataSource.class);
        DruidDataSource tenant = mock(DruidDataSource.class);
        DynamicDataSource router = router(system);
        router.addDataSources(1, tenant);
        router.closeTenantDataSources();
        verify(tenant).close();
        verify(system, never()).close();
    }

    private DynamicDataSource router(DataSource system) {
        DynamicDataSource router = new DynamicDataSource();
        router.setTargetDataSources(Map.of(0, system));
        router.setDefaultTargetDataSource(system);
        router.afterPropertiesSet();
        return router;
    }
}
