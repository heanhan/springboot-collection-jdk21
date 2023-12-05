package org.example.mongodb.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value="/user")
@RestController
public class UserController {


    @GetMapping(value="/addUserTestData")
    public ResultBody addUserTestData(){

        return ResultBody.success();
    }


}
