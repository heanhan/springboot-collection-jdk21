package com.example.ddd.cart;

import com.example.ddd.cart.domain.repository.CartRepository;
import com.example.ddd.cart.infrastructure.persistence.converter.CartConverter;
import com.example.ddd.cart.infrastructure.persistence.po.CartPO;
import com.example.ddd.cart.infrastructure.persistence.repository.CartRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.*;

/** H2 MySQL 模式验证原始迁移、初始化 upsert、JSON 快照和用户隔离。 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cart_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CartPersistenceTest.Config.class)
class CartPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.AutoConfigurationPackage
    @EntityScan(basePackageClasses = CartPO.class)
    @Import({CartRepositoryImpl.class, CartConverter.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired CartRepository repository;
    @Autowired EntityManager em;

    @Test void initializeMergeAndReload() {
        assertThat(repository.load("user", false).items()).isEmpty();
        var cart = repository.load("user", true);
        cart.add("sku", 2); repository.save(cart);
        em.flush(); em.clear();
        cart = repository.load("user", true);
        cart.add("sku", 3); repository.save(cart);
        em.flush(); em.clear();
        assertThat(repository.load("user", false).items().getFirst().quantity()).isEqualTo(5);
        assertThat(repository.load("other", false).items()).isEmpty();
        assertThat(em.find(CartPO.class, "user").version).isGreaterThan(0);
    }
}
