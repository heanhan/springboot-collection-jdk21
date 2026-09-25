package com.example.dynamic.jpa.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.example.dynamic.jpa.starter.InitDataSourceConfiguration;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceInitializationTest {
    @Test
    void devConfigurationBindsDatabaseCredentialsAndPoolSettings() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("spring.profiles.active=dev",
                        "spring.datasource.url=jdbc:h2:mem:binding",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=test-user", "spring.datasource.password=test-password")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DynamicDatabaseProperties properties = context.getBean(DynamicDatabaseProperties.class);
                    assertThat(properties.getUrl()).isEqualTo("jdbc:h2:mem:binding");
                    assertThat(properties.getDriverClassName()).isEqualTo("org.h2.Driver");
                    assertThat(properties.getUsername()).isEqualTo("test-user");
                    assertThat(properties.getPassword()).isEqualTo("test-password");
                    assertThat(properties.getMaxActive()).isEqualTo(20);
                });
    }

    @Test
    void startupClosesProbeConnectionAndDoesNotRecreateRegisteredPools() throws Exception {
        Fixture fixture = fixture();
        DruidPooledConnection connection = mock(DruidPooledConnection.class);
        when(fixture.pool.getConnection()).thenReturn(connection);
        try (var context = new GenericApplicationContext()) {
            ContextRefreshedEvent event = new ContextRefreshedEvent(context);
            fixture.initializer.onApplicationEvent(event);
            fixture.initializer.onApplicationEvent(event);
        }
        verify(connection).close();
        verify(fixture.factory, times(1)).create(fixture.tenant);
        assertThat(fixture.router.getResolvedDataSources()).containsEntry(1, fixture.pool);
    }

    @Test
    void startupFailureIsIsolatedByDefaultAndDoesNotPublishPool() throws Exception {
        Fixture fixture = fixture();
        when(fixture.pool.getConnection()).thenThrow(new SQLException("测试连接失败"));
        try (var context = new GenericApplicationContext()) {
            // 默认 fail-fast=false：单租户失败仅告警隔离，不中断启动
            assertThatCode(() -> fixture.initializer.onApplicationEvent(new ContextRefreshedEvent(context)))
                    .doesNotThrowAnyException();
        }
        verify(fixture.pool).close();
        assertThat(fixture.router.getResolvedDataSources()).doesNotContainKey(1);
    }

    @Test
    void startupFailureThrowsWhenFailFastEnabled() throws Exception {
        Fixture fixture = fixture();
        fixture.properties.getStartup().setFailFast(true);
        when(fixture.pool.getConnection()).thenThrow(new SQLException("测试连接失败"));
        try (var context = new GenericApplicationContext()) {
            assertThatThrownBy(() -> fixture.initializer.onApplicationEvent(new ContextRefreshedEvent(context)))
                    .isInstanceOf(IllegalStateException.class);
        }
        verify(fixture.pool).close();
        assertThat(fixture.router.getResolvedDataSources()).doesNotContainKey(1);
    }

    private Fixture fixture() {
        TenantData tenant = new TenantData();
        tenant.setTenantId(1);
        TenantDataInfoService service = mock(TenantDataInfoService.class);
        when(service.listActiveTenantDataInfo()).thenReturn(List.of(tenant));
        TenantDataSourceFactory factory = mock(TenantDataSourceFactory.class);
        DruidDataSource pool = mock(DruidDataSource.class);
        when(factory.create(tenant)).thenReturn(pool);
        TenantDataSourceProperties properties = new TenantDataSourceProperties();
        DynamicDataSource router = new DynamicDataSource();
        router.setTargetDataSources(Map.of(0, mock(DataSource.class)));
        router.afterPropertiesSet();
        InitDataSourceConfiguration initializer = new InitDataSourceConfiguration(service, factory, properties);
        ReflectionTestUtils.setField(initializer, "dataSource", router);
        return new Fixture(tenant, factory, properties, pool, router, initializer);
    }

    private record Fixture(TenantData tenant, TenantDataSourceFactory factory, TenantDataSourceProperties properties,
                           DruidDataSource pool, DynamicDataSource router, InitDataSourceConfiguration initializer) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DynamicDatabaseProperties.class)
    static class PropertiesConfiguration {
    }
}
