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
     * 添加租户数据源信息
     *
     * @param reqVo 请求信息
     * @return boolean
     */
    TenantData addTenantDataInfo(AddDataSourceReqVo reqVo);
}
