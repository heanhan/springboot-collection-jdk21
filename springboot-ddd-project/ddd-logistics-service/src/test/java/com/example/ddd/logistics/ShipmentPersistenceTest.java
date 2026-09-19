package com.example.ddd.logistics;

import com.example.ddd.logistics.domain.model.aggregate.Shipment;
import com.example.ddd.logistics.domain.repository.ShipmentRepository;
import com.example.ddd.logistics.infrastructure.persistence.converter.ShipmentConverter;
import com.example.ddd.logistics.infrastructure.persistence.po.ShipmentPO;
import com.example.ddd.logistics.infrastructure.persistence.repository.ShipmentRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** H2 + Flyway 验证物流 PO 映射及含时间/轨迹的完整聚合重建。 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shipment_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ShipmentPersistenceTest.Config.class)
class ShipmentPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.AutoConfigurationPackage
    @EntityScan(basePackageClasses = ShipmentPO.class)
    @Import({ShipmentRepositoryImpl.class, ShipmentConverter.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired ShipmentRepository repository;
    @Autowired EntityManager em;

    @Test void persistTracksAndDeliverAfterReload() {
        var shipment = Shipment.create("s", "o", "no", "u", "收件人", "13800000000", "地址",
                List.of(new Shipment.Item("sku", "商品", 2)), Shipment.Carrier.MOCK);
        repository.save(shipment);
        em.flush(); em.clear();
        assertThat(repository.activeIds()).containsExactly("s");
        shipment = repository.find("s", true).orElseThrow();
        assertThat(shipment.getDomainEvents()).isEmpty();
        shipment.advance(); shipment.deliver(); repository.save(shipment);
        em.flush(); em.clear();
        var loaded = repository.byOrder("o").orElseThrow();
        assertThat(loaded.state().status()).isEqualTo(Shipment.Status.DELIVERED);
        assertThat(loaded.state().tracks()).hasSize(3);
        assertThat(loaded.state().items()).containsExactly(new Shipment.Item("sku", "商品", 2));
        assertThat(repository.activeIds()).isEmpty();
    }
}
