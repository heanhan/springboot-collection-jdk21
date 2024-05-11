package com.example.elk.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Delayed;

@Slf4j
@Component
public class ElasticsearchUtils {

    @Resource
    public ElasticsearchClient elasticsearchClient;

    /**
     * 手动创建索引默认 分片
     * @param indexName
     * @return
     */
    public CreateIndexResponse createIndex(String indexName){
        CreateIndexResponse response=null;
        //创建索引  并且索引必须是小写  不然报 Invalid index name [indexName], must be lowercase
        CreateIndexRequest index = new CreateIndexRequest.Builder()
                .index(indexName)
                .build();
        //执行，获得响应
        try {
            response = elasticsearchClient.indices().create(index);
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteIndex(String indexName){


    }
}
