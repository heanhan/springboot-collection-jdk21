package com.example.dynamic.jpa.system.controller;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.security.JwtUser;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.service.TenantLifecycleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.vo.CreateTenantReq;
import com.example.dynamic.jpa.system.vo.DestroyTenantReq;
import com.example.dynamic.jpa.system.vo.RenewTenantReq;
import com.example.dynamic.jpa.system.vo.RetryProvisionReq;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantLifecycleService tenantLifecycleService;

    @GetMapping
    public ResultBody list() {
        Integer currentTenant = currentTenant();
        List<Tenant> tenants = currentTenant == SystemConstant.SYSTEM_TENANT_ID
                ? tenantService.listAllTenant()
                : List.of(requireTenant(currentTenant));
        return ResultBody.success(tenants);
    }

    @GetMapping("/{id}")
    public ResultBody get(@PathVariable Integer id) {
        assertScope(id);
        return ResultBody.success(requireTenant(id));
    }

    @PostMapping
    public ResultBody create(@Valid @RequestBody CreateTenantReq request) {
        requirePlatform();
        return ResultBody.success(tenantLifecycleService.createTenant(request));
    }

    @PutMapping("/{id}")
    public ResultBody update(@PathVariable Integer id, @Valid @RequestBody Tenant request) {
        assertScope(id);
        request.setId(id);
        return ResultBody.success(tenantService.editTenant(request));
    }

    @PostMapping("/{id}/renew")
    public ResultBody renew(@PathVariable Integer id, @Valid @RequestBody RenewTenantReq request) {
        assertScope(id);
        return ResultBody.success(tenantLifecycleService.renew(id, request.getDays()));
    }

    @PostMapping("/{id}/disable")
    public ResultBody disable(@PathVariable Integer id) {
        requirePlatform();
        return ResultBody.success(tenantLifecycleService.disable(id));
    }

    @PostMapping("/{id}/enable")
    public ResultBody enable(@PathVariable Integer id) {
        requirePlatform();
        return ResultBody.success(tenantLifecycleService.enable(id));
    }

    @PostMapping("/{id}/destroy")
    public ResultBody destroy(@PathVariable Integer id, @Valid @RequestBody DestroyTenantReq request) {
        requirePlatform();
        return ResultBody.success(tenantLifecycleService.destroy(id, request));
    }

    @PostMapping("/{id}/retry-provision")
    public ResultBody retryProvision(@PathVariable Integer id, @Valid @RequestBody(required = false) RetryProvisionReq request) {
        requirePlatform();
        return ResultBody.success(tenantLifecycleService.retryProvision(id, request));
    }

    @GetMapping("/{id}/tasks")
    public ResultBody tasks(@PathVariable Integer id) {
        assertScope(id);
        return ResultBody.success(tenantLifecycleService.listTasks(id));
    }

    @DeleteMapping("/{id}")
    public ResultBody legacyDelete(@PathVariable Integer id) {
        requirePlatform();
        return ResultBody.success(tenantService.deleteTenant(id));
    }

    private Tenant requireTenant(Integer id) {
        Tenant tenant = tenantService.getTenantById(id);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        return tenant;
    }

    private void assertScope(Integer tenantId) {
        if (currentTenant() != SystemConstant.SYSTEM_TENANT_ID
                && !currentTenant().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("不能管理其他租户");
        }
    }

    private void requirePlatform() {
        if (currentTenant() != SystemConstant.SYSTEM_TENANT_ID) {
            throw new org.springframework.security.access.AccessDeniedException("仅平台管理员可以执行此操作");
        }
    }

    private Integer currentTenant() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser user)) {
            throw new org.springframework.security.access.AccessDeniedException("登录状态无效");
        }
        return user.getUser().getTenantId();
    }
}
