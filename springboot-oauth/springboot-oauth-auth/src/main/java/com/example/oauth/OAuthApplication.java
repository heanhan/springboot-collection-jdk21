package com.example.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * OAuth2.0 授权中心微服务启动类
 * <p>
 * Spring Boot 2.7.18 + Spring Security + spring-security-oauth2 + JPA + Redis
 */
@SpringBootApplication
@EntityScan(basePackages = "com.example.oauth.entity")
@EnableJpaRepositories(basePackages = "com.example.oauth.repository")
public class OAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuthApplication.class, args);
    }
}
