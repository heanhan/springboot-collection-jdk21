package com.example.dynamic.jpa.system.controller;


import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.system.config.MyDataSource;
import com.example.dynamic.jpa.system.entity.DataSourceType;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import com.example.dynamic.jpa.security.JwtUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
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

    @PostMapping("addDataSource")
    public ResultBody addDataSource(@RequestBody @Valid AddDataSourceReqVo reqVo) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer currentTenant = principal.getUser().getTenantId();
        if (currentTenant == null || (currentTenant != 0 && !currentTenant.equals(reqVo.getTenantId()))) {
            throw new AccessDeniedException("不能管理其他租户的数据源");
        }
        return ResultBody.success(tenantDataInfoService.addTenantDataInfo(reqVo));
    }

}
