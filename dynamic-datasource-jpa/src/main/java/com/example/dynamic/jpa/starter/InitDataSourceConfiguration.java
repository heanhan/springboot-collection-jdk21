package com.example.dynamic.jpa.starter;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.DynamicDatabaseProperties;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class InitDataSourceConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    private final TenantDataInfoService tenantDataInfoService;

    private final DynamicDatabaseProperties dynamicDatabaseProperties;

    @Resource(name="multipleDataSource")
    private DataSource dataSource;

    public InitDataSourceConfiguration(TenantDataInfoService tenantDataInfoService, DynamicDatabaseProperties dynamicDatabaseProperties) {
        this.tenantDataInfoService = tenantDataInfoService;
        this.dynamicDatabaseProperties = dynamicDatabaseProperties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            log.info("----------初始化动态数据源------------");
            List<TenantData> tenantData = tenantDataInfoService.listAllTenantDataInfo();
            log.info("数据源列表:{}", tenantData);

            DynamicDataSource dynamicDataSource = (DynamicDataSource) dataSource;
            Map<Object, DataSource> resolvedDataSources = dynamicDataSource.getResolvedDataSources();

            Map<Object, Object> targetDataSources = dynamicDatabaseProperties.initDataSource(tenantData, resolvedDataSources);

            Set<Map.Entry<Object, Object>> entries = targetDataSources.entrySet();
            for (Map.Entry entry : entries) {
                try {
                    DruidDataSource druidDataSource = (DruidDataSource) entry.getValue();
                    String url = druidDataSource.getConnection().getMetaData().getURL();
                    log.debug("连接地址：{}:{}", entry.getKey(), url);
                } catch (SQLException throwAbles) {
                    throwAbles.printStackTrace();
                }
            }
            dynamicDataSource.setTargetDataSources(targetDataSources);
            dynamicDataSource.afterPropertiesSet();
        }
    }
}
