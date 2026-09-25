package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;

import java.util.List;

/**
 * @Author: zhaojh
 * @ClassName: TenantIdInfoServiceImpl
 * @Description: 租户数据库连接信息接口
 */
public interface TenantDataInfoService {

    /**
     * 获取所有租户连接信息
     *
     * @return java.util.List<TenantDataInfo>
     */
    List<TenantData> listAllTenantDataInfo();

    /**
     * 获取所属租户仍有效的连接信息
     */
    List<TenantData> listActiveTenantDataInfo();

    /**
     * 停用租户数据源并撤下路由
     */
    boolean disableTenantDataSource(Integer tenantId);

    /**
     * 添加租户数据源信息
     *
     * @param reqVo 请求信息
     * @return boolean
     */
    TenantData addTenantDataInfo(AddDataSourceReqVo reqVo);

    /**
     * 确保指定租户的连接池已注册：已注册则直接返回，否则从系统库按需构建并注册（懒加载）。
     *
     * @param tenantId 租户id
     * @return 已注册的租户数据源；租户不存在/已停用/连接失败时返回 null
     */
    javax.sql.DataSource ensureTenantDataSource(Integer tenantId);
}
