package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.ProvisioningTask;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.vo.CreateTenantReq;
import com.example.dynamic.jpa.system.vo.DestroyTenantReq;
import com.example.dynamic.jpa.system.vo.RetryProvisionReq;

import java.util.List;

public interface TenantLifecycleService {

    Tenant createTenant(CreateTenantReq request);

    Tenant retryProvision(Integer tenantId, RetryProvisionReq request);

    Tenant renew(Integer tenantId, int days);

    Tenant disable(Integer tenantId);

    Tenant enable(Integer tenantId);

    Tenant destroy(Integer tenantId, DestroyTenantReq request);

    List<ProvisioningTask> listTasks(Integer tenantId);
}
