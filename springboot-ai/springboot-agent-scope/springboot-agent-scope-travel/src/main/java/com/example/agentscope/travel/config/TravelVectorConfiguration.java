package com.example.agentscope.travel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 为 pgvector 创建独立 JDBC 连接，避免影响 Spring Data JPA 的 MySQL 数据源。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TravelVectorProperties.class)
public class TravelVectorConfiguration {

    @Bean
    public JdbcTemplate travelVectorJdbcTemplate(TravelVectorProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return new JdbcTemplate(dataSource);
    }
}
