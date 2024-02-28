package com.example.dynamic.jpa.system.config;

import com.example.dynamic.jpa.system.dao.TenantDataInfoDao;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.tenant.dao.TestDao;
import com.example.dynamic.jpa.tenant.entity.Test;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * @Author: zhaojh
 * @ClassName: MasterDatabaseProperties
 * @Description: 系统数据源
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(value = DynamicDatabaseProperties.class)
@EnableTransactionManagement
 //设置Repository所在位置
@EnableJpaRepositories(
        basePackages={"com.example.dynamic.jpa.*.dao","com.example.dynamic.jpa.*.entity"}
        , entityManagerFactoryRef = "multipleEntityManagerFactory", transactionManagerRef = "multipleTransactionManager"
)
public class DynamicDatabaseConfiguration {

    @Resource
    private DynamicDatabaseProperties dynamicDatabaseProperties;

    public DynamicDatabaseConfiguration(DynamicDatabaseProperties dynamicDatabaseProperties) {
        this.dynamicDatabaseProperties = dynamicDatabaseProperties;
    }

    @Bean
    @Primary
    public DataSource multipleDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = CollectionUtils.newHashMap(1);
        targetDataSources.put(0, systemDataSource());
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(systemDataSource());
        return dynamicDataSource;
    }

    /**
     * 系统数据源，也是默认数据源。连接的是系统数据库，系统数据源必须先加载，才能获取租户的连接信息
     *
     * @return javax.sql.DataSource
     */
    @Bean
//    @Primary
    public DataSource systemDataSource() {
        return dynamicDatabaseProperties.getBaseDataSource(null, true);
    }



    @Primary
    @Bean(name = "multipleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean masterEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();

        // Set the master data source
        em.setDataSource(multipleDataSource());

        // The master tenant entity and repository need to be scanned
        em.setPackagesToScan(
                new String[] { TenantData.class.getPackage().getName(),
                        TenantDataInfoDao.class.getPackage().getName() ,
                        Test.class.getPackage().getName(),
                        TestDao.class.getPackage().getName()
                });
        // Setting a name for the persistence unit as Spring sets it as
        // 'default' if not defined
        em.setPersistenceUnitName("masterdb-persistence-unit");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaProperties(hibernateProperties());
        log.info("Setup of masterEntityManagerFactory succeeded.");
        return em;
    }

    @Bean(name = "multipleTransactionManager")
    public JpaTransactionManager multipleTransactionManager(
            @Qualifier("multipleEntityManagerFactory") EntityManagerFactory emf) {
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
        properties.put(org.hibernate.cfg.Environment.DIALECT,
                "org.hibernate.dialect.MySQL5Dialect");
        properties.put(org.hibernate.cfg.Environment.SHOW_SQL, true);
        properties.put(org.hibernate.cfg.Environment.FORMAT_SQL, true);
        properties.put(org.hibernate.cfg.Environment.HBM2DDL_AUTO, "update");
        return properties;
    }
}
