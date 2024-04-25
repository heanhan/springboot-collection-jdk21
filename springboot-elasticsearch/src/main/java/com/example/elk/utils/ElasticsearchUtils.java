package com.example.elk.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ElasticsearchUtils {

    @Resource
    public ElasticsearchClient elasticsearchClient;

    public boolean createIndex(String indexName){
        CreateIndexResponse response=null;
        //创建索引
        CreateIndexRequest index = new CreateIndexRequest.Builder()
                .index("indexName")
                .build();
        //执行，获得响应
        try {
            response = elasticsearchClient.indices().create(index);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
