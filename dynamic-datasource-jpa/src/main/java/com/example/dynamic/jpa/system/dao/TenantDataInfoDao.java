package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.TenantData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantDataInfoDao extends BaseRepository<TenantData, Integer>, JpaSpecificationExecutor<TenantData> {

    List<TenantData> findAllByIsDelFalse();

    @Query("select td from TenantData td, Tenant t "
            + "where td.isDel = false and td.status = 'READY' and t.isDel = false and t.status = 'ACTIVE' "
            + "and (t.expiresAt is null or t.expiresAt > current timestamp) and td.tenantId = t.id")
    List<TenantData> findAllActiveTenantData();

    Optional<TenantData> findByTenantId(Integer tenantId);

    boolean existsByTenantIdAndIsDelFalse(Integer tenantId);
}
