package org.example.validate.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.example.validate.pojo.vo.UserVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单的类属性进行校验
 */


@Slf4j
@RequestMapping(value = "/user")
@RestController
public class UserController {


    @PostMapping(value = "/validateUserAttribution")
    public ResultBody validateUserAttribution(@RequestBody UserVo userVo){
        log.info("进入方法内部");
        log.info("参数校验成功！");
        return ResultBody.success();
    }
}
