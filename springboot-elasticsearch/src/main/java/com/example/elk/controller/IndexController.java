package com.example.elk.controller;

import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import com.alibaba.fastjson.JSONObject;
import com.example.common.result.ResultBody;
import com.example.elk.utils.ElasticsearchUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/index")
@RestController
public class IndexController {

    @Resource
    private ElasticsearchUtils elasticsearchUtils;

    /**
     * 创建自定义的索引
     * @param object json参数
     * @return ResultBody<CreateIndexResponse>
     */
    @PostMapping(value = "/createIndex")
    public ResultBody<CreateIndexResponse> createIndex(@RequestBody JSONObject object) {
        Assert.hasText(object.getString("indexName"),"需要创建的索引参数不能为空!");
        String indexName = object.getString("indexName");
        CreateIndexResponse index = elasticsearchUtils.createIndex(indexName);
        return ResultBody.success(index);
    }


}
