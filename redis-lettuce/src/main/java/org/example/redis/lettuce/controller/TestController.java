package org.example.redis.lettuce.controller;


import com.example.common.result.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value = "/test")
@RestController
public class TestController {

    @GetMapping(value = "/getConnection")
    public ResultBody getConnection(){
        log.info("进入方法内部");
        return ResultBody.success();
    }
}
