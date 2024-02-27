package com.example.redission.service.impl;

import com.example.redission.entity.Order;
import com.example.redission.entity.Storage;
import com.example.redission.service.OrderService;
import com.example.redission.service.ReduceService;
import com.example.redission.service.StorageService;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReduceServiceImpl implements ReduceService {

    @Resource
    private StorageService storageService;

    @Resource
    private OrderService orderService;
    /**
     * 用户抢购商品
     *
     * @param produceId 产品id
     * @param userId 用户id
     */
    @Transactional
    @Override
    public ResultBody handleReduceGoods(Integer produceId,Integer userId) {
        //1、先获取库存
        Storage storage=storageService.findStorageById(produceId);
        //如果库存为0  则表明抢购结束
        if(storage.getNumber()<0){
            return ResultBody.success("很抱歉，抢购失败，请再次抢购");
        }
        //2、创建订单
        Order order=new Order();
        order.setPid(storage.getId());
        order.setUserId(userId);
        orderService.addOrderInfo(order);
        //3、减库存
        Integer flag=storageService.reduceStorageNumber(produceId);
        if(flag<0){
            ResultBody.error("扣减库存异常，到时抢购失败");
        }
        return ResultBody.success("请购成功");
    }
}
