package com.example.dynamic.jpa.tenant.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.DynamicDatabaseProperties;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import com.example.dynamic.jpa.tenant.entity.Test;
import com.example.dynamic.jpa.tenant.service.TestService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @Author: zhaojh
 * @ClassName: TestController
 * @Description: 用户测试搭建的框架的数据源是否正确
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    private TestService testService;

    @MyDataSource(type = DataSourceType.TENANT)
    @GetMapping("getTestById")
    public ResultBody getTestById(Integer id, Integer data) {
        LoginInfo loginInfo = LoginInfoHolder.getTenant();
        if (loginInfo == null || (data != null && !data.equals(loginInfo.getTenantId()))) {
            throw new org.springframework.security.access.AccessDeniedException("不能访问其他租户的数据");
        }
        Test testById = testService.getTestById(id);
        return ResultBody.success(testById);
    }

}
