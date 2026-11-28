package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.security.JwtUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/datasource")
public class DataSourceController {

    @Resource
    private TenantDataInfoService tenantDataInfoService;

    @GetMapping
    public ResultBody getDataSource(@PathVariable Integer tenantId) {
        assertScope(tenantId);
        TenantData data = tenantDataInfoService.getTenantData(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("租户数据源不存在"));
        return ResultBody.success(data);
    }

    @PostMapping("/test")
    public ResultBody testDataSource(@PathVariable Integer tenantId) {
        assertScope(tenantId);
        tenantDataInfoService.testTenantDataSource(tenantId);
        return ResultBody.success("连接测试成功");
    }

    private void assertScope(Integer tenantId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser user)) {
            throw new org.springframework.security.access.AccessDeniedException("登录状态无效");
        }
        if (user.getUser().getTenantId() != 0 && !user.getUser().getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("不能查看其他租户数据源");
        }
    }
}
