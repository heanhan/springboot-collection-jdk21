package com.example.test.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/starter")
@RestController
public class TestStarterController {

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
