package com.example.elk.controller;

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

    @PostMapping(value = "/createIndex")
    public ResultBody<String> createIndex(@RequestBody JSONObject object) {
        Assert.hasText(object.getString("indexName"),"需要创建的索引参数不能为空!");
        String indexName = object.getString("indexName");
        boolean index = elasticsearchUtils.createIndex(indexName);
        if(index){
            ResultBody.success("索引创建成功！");
        }
        return ResultBody.error("索引创建失败！");
    }
}
