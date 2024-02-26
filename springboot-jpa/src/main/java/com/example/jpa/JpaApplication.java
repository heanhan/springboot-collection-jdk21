package com.example.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Hello world!
 *
 */
@EnableJpaRepositories
@SpringBootApplication
public class JpaApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(JpaApplication.class,args);
    }
}
