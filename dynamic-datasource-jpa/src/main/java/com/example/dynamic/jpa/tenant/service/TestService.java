package com.example.dynamic.jpa.tenant.service;


import com.example.dynamic.jpa.tenant.entity.Test;

/**
 * <p>
 * 系統用戶表 服务类
 * </p>
 *
 * @author zhaojh
 * @since 2022-11-04
 */
public interface TestService {

    Test getTestById(Integer id);
}
