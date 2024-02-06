package org.example.mongodb.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.example.mongodb.entity.User;
import org.example.mongodb.service.UserService;
import org.example.mongodb.vo.UserVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping(value="/user")
@RestController
public class UserController {

    @Resource
    private UserService userService;


    @GetMapping(value="/addUserTestData")
    public ResultBody addUserTestData(){

        return ResultBody.success();
    }

    @PostMapping(value="/findAllUsers")
    public ResultBody findAllUsers(@RequestBody UserVo userVo){
        try{
            List<User> userList = userService.findAllUsers(userVo);
            return ResultBody.success(userList);
        }catch (Exception e){
            return ResultBody.error("查询异常");
        }

    }


}
