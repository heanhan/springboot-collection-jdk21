package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.TenantData;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TenantDataInfoService {

    List<TenantData> listAllTenantDataInfo();

    List<TenantData> listActiveTenantDataInfo();

    Optional<TenantData> getTenantData(Integer tenantId);

    TenantData saveTenantData(TenantData tenantData);

    boolean disableTenantDataSource(Integer tenantId);

    DataSource activateTenantDataSource(Integer tenantId);

    DataSource ensureTenantDataSource(Integer tenantId);

    boolean testTenantDataSource(Integer tenantId);

    Map<String, Integer> getPoolStatistics();
}
