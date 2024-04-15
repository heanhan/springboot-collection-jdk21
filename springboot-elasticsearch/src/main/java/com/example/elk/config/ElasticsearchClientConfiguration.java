package com.example.elk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhaojh
 * Date: 2024/3/20 14:59
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties({ElasticsearchProperties.class})
@ConditionalOnClass({ElasticsearchClient.class, ElasticsearchTransport.class})
public class ElasticsearchClientConfiguration {


    private final ElasticsearchProperties elasticsearchProperties;

    public ElasticsearchClientConfiguration(ElasticsearchProperties elasticsearchProperties) {
        log.info("框架 elasticsearch-client-starter elasticsearchProperties 装载开始");
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        log.info("框架 elasticsearch-client-starter elasticsearchClient 装载开始");
        RestClient restClient = ElasticsearchConfig.buildWithProperties(elasticsearchProperties);
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
