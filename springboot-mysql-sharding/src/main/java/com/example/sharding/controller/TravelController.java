package com.example.sharding.controller;

import com.example.common.result.ResultBody;
import com.example.sharding.service.TravelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/travel")
@RestController
public class TravelController {

    @Resource
    private TravelService travelService;

    @PostMapping("/findTravelById")
    public ResultBody findTravelById(){
        return ResultBody.success();
    }
}
