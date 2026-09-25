package com.example.dynamic.jpa.system.dao;


import com.example.dynamic.jpa.system.entity.TenantData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 用户表 数据层
 *
 * @author xiaonao
 */

@Repository
public interface TenantDataInfoDao extends BaseRepository<TenantData,Integer>, JpaSpecificationExecutor<TenantData> {

    java.util.List<TenantData> findAllByIsDelFalse();

    @Query("select td from TenantData td, Tenant t "
            + "where td.isDel = false and t.isDel = false and td.tenantId = t.id")
    java.util.List<TenantData> findAllActiveTenantData();

    java.util.Optional<TenantData> findByTenantId(Integer tenantId);

    boolean existsByTenantIdAndIsDelFalse(Integer tenantId);

}
