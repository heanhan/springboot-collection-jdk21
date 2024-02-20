package org.example.redis.lettuce.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.example.redis.lettuce.service.CategoryService;
import org.example.redis.lettuce.vo.CategoryVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value = "/category")
@RestController
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 查询分类  动态掉件查询
     * @return
     */
    @PostMapping(value = "/findCategoryByCondition")
    public ResultBody findCategoryByCondition(@RequestBody CategoryVo categoryVo){

        return ResultBody.success();
    }


}
