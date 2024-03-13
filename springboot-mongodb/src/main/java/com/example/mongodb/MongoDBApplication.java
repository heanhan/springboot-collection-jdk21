package com.example.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Hello world!
 *
 */
@EnableMongoRepositories
@SpringBootApplication
public class MongoDBApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(MongoDBApplication.class,args);
    }
}
