package com.example.ddd.product.infrastructure.config;

import com.example.ddd.product.domain.repository.BrandRepository;
import com.example.ddd.product.domain.repository.CategoryRepository;
import com.example.ddd.product.domain.service.ProductPublishService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务装配配置。
 *
 * <p><b>为什么领域服务不用 {@code @Service} 注解？</b>
 * 领域层禁止依赖 Spring 注解，否则领域模型会被基础设施污染。
 * 通过本配置类在 infrastructure 层显式装配 Bean，保持领域层纯粹。</p>
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public ProductPublishService productPublishService(CategoryRepository categoryRepository,
                                                       BrandRepository brandRepository) {
        return new ProductPublishService(categoryRepository, brandRepository);
    }
}
