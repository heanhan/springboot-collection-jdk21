package org.example.start.file.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;


@Slf4j
@RestController
@RequestMapping(value = "/test")
public class TestController {

    @Value("${spring.selfValue}")
    private String selfValue;

    @GetMapping(value = "/getYmlInfo")
    @ResponseBody
    public String getYmlInfo() {

        //tomcat路径
//        String property = System.getProperty("catalina.home");
        String property = System.getProperty("user.dir");
//        String path = property + File.separator + "conf" + File.separator + "application.yml";
        String path = property + File.separator + "webapps" + File.separator + "application.yml";
        //Springboot读取yml配置
        log.info("进入方法内部。。。。。获取当前配置文件的信息");
        return "is success";
    }
}
