package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.Tenant;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * saas租户 Mapper 接口
 * </p>
 *
 * @author zhaojh
 */

@Repository
public interface TenantDao extends BaseRepository<Tenant,String>, JpaSpecificationExecutor<Tenant> {

}
