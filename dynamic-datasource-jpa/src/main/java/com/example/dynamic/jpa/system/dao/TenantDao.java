package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Tenant;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantDao extends BaseRepository<Tenant, Integer>, JpaSpecificationExecutor<Tenant> {

    Optional<Tenant> findByIdAndIsDelFalse(Integer id);

    Optional<Tenant> findByTenantCodeAndIsDelFalse(String tenantCode);

    Optional<Tenant> findByTenantNameAndIsDelFalse(String tenantName);

    List<Tenant> findAllByIsDelFalseOrderByIdDesc();

    boolean existsByTenantCodeAndIsDelFalse(String tenantCode);

    boolean existsByPhoneAndIdNotAndIsDelFalse(String phone, Integer id);

    boolean existsByPhoneAndIsDelFalse(String phone);
}
