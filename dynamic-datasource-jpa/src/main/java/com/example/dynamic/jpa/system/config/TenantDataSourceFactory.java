package com.example.dynamic.jpa.system.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.system.entity.TenantData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * 租户数据源工厂：以系统库 JDBC 基础参数为骨架，叠加租户连接池独立调优与可选密码解密，
 * 构建租户专属 {@link DruidDataSource}。
 *
 * <p>与系统库连接池（{@link DynamicDatabaseProperties#getBaseDataSource}）分离，
 * 使租户池可采用小初始连接/懒建连策略，避免租户数量增长引发连接资源爆炸。</p>
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class TenantDataSourceFactory {

    private final DynamicDatabaseProperties base;
    private final TenantDataSourceProperties options;

    public TenantDataSourceFactory(DynamicDatabaseProperties base, TenantDataSourceProperties options) {
        this.base = base;
        this.options = options;
    }

    /**
     * 为租户连接信息构建 Druid 连接池。
     *
     * @param tenant 租户连接信息（密码可能为密文，取决于是否开启加密）
     */
    public DruidDataSource create(TenantData tenant) {
        DruidDataSource dds = new DruidDataSource();
        dds.setUrl(tenant.getUrl());
        dds.setUsername(tenant.getUsername());
        // 加密开启时库中为密文，交由 Druid ConfigFilter 用公钥解密；否则明文直用
        dds.setPassword(tenant.getPassword());
        dds.setDriverClassName(base.getDriverClassName());

        // 租户池独立调优：小初始连接、懒建连
        TenantDataSourceProperties.TenantPool pool = options.getTenantPool();
        dds.setInitialSize(pool.getInitialSize());
        dds.setMinIdle(pool.getMinIdle());
        dds.setMaxActive(pool.getMaxActive());
        dds.setMaxWait(pool.getMaxWait());

        // 连接有效性/回收策略沿用系统库基础配置
        dds.setValidationQuery(base.getValidationQuery());
        dds.setTestWhileIdle(base.isTestWhileIdle());
        dds.setTestOnBorrow(base.isTestOnBorrow());
        dds.setTestOnReturn(base.isTestOnReturn());
        dds.setTimeBetweenEvictionRunsMillis(base.getTimeBetweenEvictionRunsMillis());
        dds.setMinEvictableIdleTimeMillis(base.getMinEvictableIdleTimeMillis());
        dds.setPoolPreparedStatements(base.isPoolPreparedStatements());
        dds.setMaxPoolPreparedStatementPerConnectionSize(base.getMaxPoolPreparedStatementPerConnectionSize());

        String filters = base.getFilters();
        if (options.getCrypto().isEnabled()) {
            // 追加 config filter 并配置解密参数（config.decrypt.key 为公钥）
            filters = StringUtils.isBlank(filters) ? "config" : filters + ",config";
            dds.setConnectionProperties("config.decrypt=true;config.decrypt.key=" + options.getCrypto().getPublicKey());
        }
        try {
            dds.setFilters(filters);
        } catch (SQLException e) {
            log.error("租户数据源 filters 设置失败: {}", e.getMessage(), e);
        }
        return dds;
    }
}
