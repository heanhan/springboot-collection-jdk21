package org.example.mongodb.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/test")
public class TestController {


    @GetMapping(value = "/getConnection")
    public ResultBody getConnection(){
        log.info("进入方法内部");
        return ResultBody.success();
    }
}
