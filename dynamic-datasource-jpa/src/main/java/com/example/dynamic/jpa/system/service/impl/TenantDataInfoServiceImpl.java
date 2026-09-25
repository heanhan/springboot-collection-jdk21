package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.LoginInfoHolder;
import com.example.dynamic.jpa.system.config.TenantDataSourceFactory;
import com.example.dynamic.jpa.system.config.TenantPasswordCipher;
import com.example.dynamic.jpa.system.config.cluster.DataSourceChangeEvent;
import com.example.dynamic.jpa.system.config.cluster.DataSourceChangePublisher;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.dao.TenantDataInfoDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.vo.AddDataSourceReqVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/**
 * @Author: zhaojh
 * @ClassName: TenantIdInfoServiceImpl
 * @Description: 租户数据库连接信息实现类
 */
@Slf4j
@Service
public class TenantDataInfoServiceImpl implements TenantDataInfoService {

    @Resource
    private TenantDao tenantDao;

    @Resource
    private TenantDataInfoDao tenantDataInfoDao;

    @Qualifier("multipleDataSource")
    @Autowired
    private DynamicDataSource dataSource;

    @Resource
    private TenantDataSourceFactory tenantDataSourceFactory;

    @Resource
    private TenantPasswordCipher passwordCipher;

    @Resource
    private DataSourceChangePublisher changePublisher;

    @Override
    public List<TenantData> listAllTenantDataInfo() {
        return tenantDataInfoDao.findAllByIsDelFalse();
    }

    @Override
    public List<TenantData> listActiveTenantDataInfo() {
        return tenantDataInfoDao.findAllActiveTenantData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTenantDataSource(Integer tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BaseException("租户id必须大于0");
        }
        DynamicDataSource router = dataSource;
        TenantData tenantData = tenantDataInfoDao.findByTenantId(tenantId).orElse(null);
        if (tenantData != null && !Boolean.TRUE.equals(tenantData.getIsDel())) {
            tenantData.setIsDel(true);
            tenantData.setUpdateTime(java.time.LocalDateTime.now());
            tenantDataInfoDao.saveAndFlush(tenantData);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                router.removeDataSource(tenantId);
                // 广播至其它实例，同步摘除路由
                changePublisher.publish(DataSourceChangeEvent.remove(tenantId));
            }
        });
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantData addTenantDataInfo(AddDataSourceReqVo reqVo) {
        if (reqVo.getTenantId() == null || reqVo.getTenantId() <= 0) {
            throw new BaseException("租户id必须大于0");
        }
        DynamicDataSource router = dataSource;
        if (router.getResolvedDataSources().containsKey(reqVo.getTenantId())
                || tenantDataInfoDao.existsByTenantIdAndIsDelFalse(reqVo.getTenantId())) {
            throw new BaseException("租户数据源已存在");
        }
        Tenant tenant = tenantDao.findByIdAndIsDelFalse(reqVo.getTenantId()).orElse(null);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        TenantData tenantData = new TenantData();
        tenantData.setTenantId(reqVo.getTenantId());
        tenantData.setUrl(reqVo.getUrl());
        tenantData.setUsername(reqVo.getUsername());
        // 连接密码加密入库（未开启加密时原样存储），运行期由 Druid ConfigFilter 解密
        tenantData.setPassword(passwordCipher.encrypt(reqVo.getPassword()));
        tenantData.setCreateTime(java.time.LocalDateTime.now());
        DruidDataSource pool = tenantDataSourceFactory.create(tenantData);
        try {
            try (java.sql.Connection connection = pool.getConnection()) {
                connection.getMetaData();
            }
            TenantData saved = tenantDataInfoDao.saveAndFlush(tenantData);
            // 添加租户数据源之后 立即加载使其生效
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        router.addDataSources(tenant.getId(), pool);
                    } catch (RuntimeException e) {
                        pool.close();
                        throw e;
                    }
                    // 广播至其它实例，同步新增路由
                    changePublisher.publish(DataSourceChangeEvent.add(tenant.getId()));
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        pool.close();
                    }
                }
            });
            return saved;
        } catch (java.sql.SQLException e) {
            pool.close();
            throw new BaseException("租户数据库连接失败");
        } catch (RuntimeException e) {
            pool.close();
            throw e;
        }
    }

    @Override
    public javax.sql.DataSource ensureTenantDataSource(Integer tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return null;
        }
        DynamicDataSource router = dataSource;
        javax.sql.DataSource existing = router.getResolvedDataSources().get(tenantId);
        if (existing != null) {
            return existing;
        }
        // 查询系统库需系统上下文（调用方可能处于任意租户上下文或无上下文）
        TenantData tenantData = LoginInfoHolder.callAsSystem(
                () -> tenantDataInfoDao.findByTenantId(tenantId).orElse(null));
        if (tenantData == null || Boolean.TRUE.equals(tenantData.getIsDel())) {
            return null;
        }
        DruidDataSource pool = tenantDataSourceFactory.create(tenantData);
        try {
            try (java.sql.Connection connection = pool.getConnection()) {
                connection.getMetaData();
            }
        } catch (Exception e) {
            pool.close();
            log.error("懒加载租户数据源连接失败，已隔离 tenantId={}: {}", tenantId, e.getMessage());
            return null;
        }
        try {
            router.addDataSources(tenantId, pool);
        } catch (RuntimeException e) {
            // 并发下可能已被其它线程注册，关闭多余连接池即可
            pool.close();
        }
        return router.getResolvedDataSources().get(tenantId);
    }
}
