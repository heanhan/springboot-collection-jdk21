package com.example.dynamic.jpa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableJpaAuditing // 使用jpa自动赋值
@Slf4j
public class DynamicJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicJpaApplication.class, args);
    }

}
