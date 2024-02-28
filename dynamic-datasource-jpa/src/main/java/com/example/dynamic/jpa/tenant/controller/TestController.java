package com.example.dynamic.jpa.tenant.controller;

import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.DynamicDatabaseProperties;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.vo.LoginInfo;
import com.example.dynamic.jpa.tenant.entity.Test;
import com.example.dynamic.jpa.tenant.service.TestService;
import jakarta.annotation.Resource;
import org.example.commons.result.ResultBody;
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

    @Resource(name = "multipleDataSource")
    private DataSource dataSource;

    private final DynamicDatabaseProperties dynamicDatabaseProperties;

    @Resource
    private TestService testService;

    public TestController(DynamicDatabaseProperties dynamicDatabaseProperties) {
        this.dynamicDatabaseProperties = dynamicDatabaseProperties;
    }

    @MyDataSource(type= DataSourceType.TENANT,value = 2)
    @GetMapping("getTestById")
    public ResultBody getTestById(Integer id, Integer data) {
        DynamicDataSource dynamicDataSource = (DynamicDataSource) dataSource;
        Map<Object, DataSource> resolvedDataSources = dynamicDataSource.getResolvedDataSources();
        LoginInfo loginInfo =new LoginInfo();
        loginInfo.setTenantId(data);
        LoginInfoHolder.setTenant(loginInfo);
        Test testById = testService.getTestById(id);
        LoginInfoHolder.clear();
        return ResultBody.success(testById);
    }

}
