package com.example.minio;

import com.example.web.exceptions.GlobalExceptionHandler;
import com.example.web.exceptions.ValidationExceptionHandle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class MinioApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(MinioApplication.class,args);
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler(){
        return new GlobalExceptionHandler();
    }

    @Bean
    public ValidationExceptionHandle validationExceptionHandle(){
        return new ValidationExceptionHandle();
    }
}
