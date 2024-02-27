package com.example.redission.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    //使用properties中的配置
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    @Value("${spring.data.redis.password}")
    private String pwd;

    @Value("${spring.data.redis.database}")
    private Integer database;


    @Bean
    public Redisson redisson(){
        //此为单机模式-SingleServer
        Config config=new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port).setDatabase(database).setPassword(pwd);
        return (Redisson)Redisson.create(config);
    }
}