package com.example.ddd.inventory.infrastructure.config;

import com.example.ddd.inventory.domain.repository.StockRepository;
import com.example.ddd.inventory.domain.repository.WarehouseRepository;
import com.example.ddd.inventory.domain.service.StockReservationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 领域服务与事务模板装配配置。
 *
 * <p><b>为什么领域服务不用 {@code @Service}？</b>
 * 领域层禁止依赖 Spring 注解，否则领域模型被基础设施污染。
 * 通过本配置类在 infrastructure 层显式装配 Bean，保持领域层纯粹（DIP 依赖倒置）。</p>
 *
 * <p><b>为什么暴露 {@link TransactionTemplate}？</b>
 * {@code StockApplicationService} 需要在乐观锁冲突时"事务外重试"，
 * 用编程式事务把每次尝试包成独立事务，比 {@code @Transactional} 更适合重试语义。</p>
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public StockReservationService stockReservationService(StockRepository stockRepository,
                                                           WarehouseRepository warehouseRepository) {
        return new StockReservationService(stockRepository, warehouseRepository);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
