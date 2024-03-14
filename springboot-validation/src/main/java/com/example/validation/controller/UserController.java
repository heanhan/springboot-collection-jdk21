package com.example.validation.controller;

import com.example.common.result.ResultBody;
import com.example.validation.model.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value = "/user")
@RestController
public class UserController {


    /**
     * 简单的雷属性进行校验
     * @param userVo
     * @return
     */
    @PostMapping(value = "/validateUserAttribution")
    public ResultBody validateUserAttribution(@RequestBody @Validated UserVo userVo){
        log.info("进入方法内部");
        log.info("参数校验成功！");
        return ResultBody.success();
    }
}
