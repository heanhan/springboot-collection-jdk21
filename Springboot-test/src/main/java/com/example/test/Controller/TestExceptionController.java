package com.example.test.Controller;


import com.example.common.result.ResultBody;
import com.example.test.pojo.TestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "测试异常模块")
@RequestMapping("/exception")
@RestController
public class TestExceptionController {

    @Operation(summary = "返回异常")
    @PostMapping(value = "/testException1")
    public ResultBody testException1(@RequestBody TestEntity test) {
        double value = 1 / 0;
        return ResultBody.success();
    }
}
