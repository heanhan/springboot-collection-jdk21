package com.example.redission.service;

import org.example.commons.result.ResultBody;

/**
 * 用户抢购商品服务
 */
public interface ReduceService {

    /**
     * 用户抢购商品
     * @param id
     */
    ResultBody handleReduceGoods(Integer id,Integer userId);
}
