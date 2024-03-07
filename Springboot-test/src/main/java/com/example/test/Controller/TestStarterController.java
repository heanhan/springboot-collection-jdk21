package com.example.test.Controller;

import com.example.common.result.ResultBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "starter模式-测试模块")
@Slf4j
@RequestMapping("/starter")
@RestController
public class TestStarterController {

    @Operation(summary = "普通body请求")
    @PostMapping(value = "/test1")
    public ResultBody test1(){
        return ResultBody.success();
    }

//    @Value(value = "${movie.site.name}")
//    private String name;
//
//    @Value(value = "${movie.site.url}")
//    private String url;
//
//    @GetMapping(value = "/getStarterValue")
//    public String getStarterValue(){
//        log.info("打印从配置文件里的参数-> 网站名称：{}，网址：{}",name,url);
//        return "网站："+name+" -->网址 :"+url;
//    }
}
