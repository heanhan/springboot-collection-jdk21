package com.example.dynamic.jpa.system.dao;


import com.example.dynamic.jpa.system.entity.TenantData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户表 数据层
 *
 * @author xiaonao
 */

@Repository
public interface TenantDataInfoDao extends BaseRepository<TenantData,Integer>, JpaSpecificationExecutor<TenantData> {

}
