package com.example.mybatis;

import com.example.web.exceptions.GlobalExceptionHandler;
import com.example.web.exceptions.ValidationExceptionHandle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @Description : 启动类
 * @Author : zhaojh
 * @Date : 2024/7/6 09:24
 */

@SpringBootApplication
public class MybatisPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisPlusApplication.class,args);
    }

    /**
     * 注入 全局异常的 bean
     * @return
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }

    @Bean
    public ValidationExceptionHandle validationExceptionHandle(){
        return new ValidationExceptionHandle();
    }
}
