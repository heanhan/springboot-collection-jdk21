package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.DynamicDatabaseProperties;
import com.example.dynamic.jpa.system.dao.TenantDataInfoDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhaojh
 * @ClassName: TenantIdInfoServiceImpl
 * @Description: 租户数据库连接信息实现类
 */
@Service
public class TenantDataInfoServiceImpl implements TenantDataInfoService {

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantDataInfoDao tenantDataInfoDao;

    @Qualifier("multipleDataSource")
    @Autowired
    private DataSource dataSource;

    private final DynamicDatabaseProperties dynamicDatabaseProperties;

    public TenantDataInfoServiceImpl(DynamicDatabaseProperties dynamicDatabaseProperties) {
        this.dynamicDatabaseProperties = dynamicDatabaseProperties;
    }

    @Override
    public List<TenantData> listAllTenantDataInfo() {
        //todo
        List<TenantData> all = tenantDataInfoDao.findAll();
        return all;
    }

    @Override
    public TenantData addTenantDataInfo(AddDataSourceReqVo reqVo) {
        Tenant tenant = tenantService.getTenantById(reqVo.getTenantId());
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        TenantData tenantData = new TenantData();
        tenantData.setTenantId(reqVo.getTenantId());
        tenantData.setUrl(reqVo.getUrl());
        tenantData.setUsername(reqVo.getUsername());
        tenantData.setPassword(reqVo.getPassword());
        TenantData save = tenantDataInfoDao.save(tenantData);
        // 添加租户数据源之后 立即加载使其生效
        if (!ObjectUtils.isEmpty(save)) {
            DynamicDataSource dynamicDataSource = (DynamicDataSource) dataSource;
            Map<Object, DataSource> resolvedDataSources = dynamicDataSource.getResolvedDataSources();
            dynamicDataSource.addDataSources(tenant.getId(), dynamicDatabaseProperties.getBaseDataSource(tenantData, false));
        }
        return save;
    }
}
