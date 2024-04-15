package com.example.elk.repository;


import com.example.elk.entity.Shop;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 使用 spring-data 的方式进行处理
 */
@Repository
public interface ShopRepository extends ElasticsearchRepository<Shop, String> {


}
