package com.example.dynamic.jpa.system.config;

import com.example.dynamic.jpa.tenant.dao.TestDao;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackageClasses = TestDao.class,
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class TenantRepositoryConfiguration {
}
