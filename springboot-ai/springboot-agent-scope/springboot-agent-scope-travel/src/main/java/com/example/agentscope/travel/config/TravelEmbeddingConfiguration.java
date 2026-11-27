package com.example.agentscope.travel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 创建旅游知识库专用的 Embedding HTTP 客户端。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TravelEmbeddingProperties.class)
public class TravelEmbeddingConfiguration {

    @Bean
    public RestClient travelEmbeddingRestClient(TravelEmbeddingProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
