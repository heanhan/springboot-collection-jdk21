package com.example.dynamic.jpa.system.controller;


import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @Author: zhaojh
 * @ClassName: TestController
 * @Description: 数据源接口
 */
@RestController
@RequestMapping("/api/dataSource")
@MyDataSource(type = DataSourceType.SYSTEM)
public class DataSourceController {

    @Autowired
    private TenantDataInfoService tenantDataInfoService;

    @GetMapping("addDataSource")
    public ResultBody addDataSource(@RequestBody @Valid AddDataSourceReqVo reqVo) {
        return ResultBody.success(tenantDataInfoService.addTenantDataInfo(reqVo));
    }

}
