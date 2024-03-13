package com.example.mongodb.controller;

import com.example.common.result.ResultBody;
import com.example.mongodb.entity.User;
import com.example.mongodb.model.vo.UserVo;
import com.example.mongodb.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
