package com.example.redission.controller;


import com.example.common.enums.CommonEnum;
import com.example.common.result.ResultBody;
import com.example.redission.service.ReduceService;
import com.example.redission.vo.ReduceRequestVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RequestMapping("/api")
@RestController
public class RedissionController {

    @Resource
    private Redisson redisson;

    @Resource
    private ReduceService reduceService;

    @PostMapping(value = "/reduceStockByRedisson")
    public ResultBody reduceStockByRedisson(@RequestBody ReduceRequestVo vo){
        Random random = new Random();
        // 生成1到20之间（包括1但不包括20）的随机整数
        int randomNum = random.nextInt(100);
        vo.setUserId(randomNum);
        String lockKey = "lockKey";
        //1.获取锁对象
        RLock redissonLock = redisson.getLock(lockKey);
        try {
            //2.使用redissonLock加锁
            redissonLock.lock();
            //加锁成功（获得了锁），那么执行业务逻辑
            log.info("使用Redisson加锁成功！执行业务代码=>");
            reduceService.handleReduceGoods(vo.getProduceId(),vo.getUserId());
            return ResultBody.success("购买成功");
        } catch (Exception e) {
            log.info("用户抢购业务出现系统异常：{}",e.getMessage());
            return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
        } finally {
            //3.释放锁
            redissonLock.unlock();
            log.info("<=业务代码执行完毕，释放当前锁！");
        }

    }

    @PostMapping(value = "/countAccount")
    public ResultBody countAccount(){
        Map<String,String> map=new HashMap<>();
        return ResultBody.success();
    }



}
