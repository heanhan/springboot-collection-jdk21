package com.example.dynamic.jpa.system.config;

import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.tenant.dao.TestDao;
import com.example.dynamic.jpa.tenant.entity.Test;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@Configuration
@EnableConfigurationProperties(value = {DynamicDatabaseProperties.class, TenantDataSourceProperties.class})
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackageClasses = UserDao.class,
        entityManagerFactoryRef = "systemEntityManagerFactory",
        transactionManagerRef = "systemTransactionManager"
)
public class DynamicDatabaseConfiguration {

    private final DynamicDatabaseProperties dynamicDatabaseProperties;
    private final TenantDataSourceProperties tenantDataSourceProperties;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    public DynamicDatabaseConfiguration(DynamicDatabaseProperties dynamicDatabaseProperties,
                                        TenantDataSourceProperties tenantDataSourceProperties) {
        this.dynamicDatabaseProperties = dynamicDatabaseProperties;
        this.tenantDataSourceProperties = tenantDataSourceProperties;
    }

    @Bean
    @Primary
    public DataSource systemDataSource() {
        return dynamicDatabaseProperties.getBaseDataSource(null, true);
    }

    @Bean(name = "tenantDataSource")
    public DynamicDataSource tenantDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setMaxTotalConnections(tenantDataSourceProperties.getTenantPool().getMaxTotalConnections());
        dynamicDataSource.setTargetDataSources(java.util.Map.of());
        return dynamicDataSource;
    }

    @Bean(name = "systemEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean systemEntityManagerFactory(
            @Qualifier("systemDataSource") DataSource systemDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(systemDataSource);
        em.setPackagesToScan(
                Tenant.class.getPackage().getName(),
                TenantData.class.getPackage().getName(),
                User.class.getPackage().getName()
        );
        em.setPersistenceUnitName("system-persistence-unit");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());
        return em;
    }

    @Bean(name = "systemTransactionManager")
    @Primary
    public JpaTransactionManager systemTransactionManager(
            @Qualifier("systemEntityManagerFactory") EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            @Qualifier("tenantDataSource") DataSource tenantDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(tenantDataSource);
        em.setPackagesToScan(
                Test.class.getPackage().getName(),
                TestDao.class.getPackage().getName()
        );
        em.setPersistenceUnitName("tenant-persistence-unit");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties properties = hibernateProperties();
        properties.put(org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT, false);
        em.setJpaProperties(properties);
        return em;
    }

    @Bean(name = "tenantTransactionManager")
    public JpaTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put(org.hibernate.cfg.Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
        properties.put(org.hibernate.cfg.Environment.SHOW_SQL, showSql);
        properties.put(org.hibernate.cfg.Environment.FORMAT_SQL, showSql);
        properties.put(org.hibernate.cfg.Environment.HBM2DDL_AUTO, ddlAuto);
        return properties;
    }
}
