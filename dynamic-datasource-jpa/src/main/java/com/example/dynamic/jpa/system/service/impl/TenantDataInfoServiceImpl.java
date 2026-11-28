package com.example.dynamic.jpa.system.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.config.DynamicDataSource;
import com.example.dynamic.jpa.system.config.TenantDataSourceFactory;
import com.example.dynamic.jpa.system.config.cluster.DataSourceChangeEvent;
import com.example.dynamic.jpa.system.config.cluster.DataSourceChangePublisher;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.dao.TenantDataInfoDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.entity.TenantData;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TenantDataInfoServiceImpl implements TenantDataInfoService {

    @Resource
    private TenantDao tenantDao;

    @Resource
    private TenantDataInfoDao tenantDataInfoDao;

    @Resource(name = "tenantDataSource")
    private DynamicDataSource dataSource;

    @Resource
    private TenantDataSourceFactory tenantDataSourceFactory;

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
    public Optional<TenantData> getTenantData(Integer tenantId) {
        return tenantDataInfoDao.findByTenantId(tenantId);
    }

    @Override
    @Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
    public TenantData saveTenantData(TenantData tenantData) {
        return tenantDataInfoDao.saveAndFlush(tenantData);
    }

    @Override
    @Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
    public boolean disableTenantDataSource(Integer tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BaseException("租户id必须大于0");
        }
        TenantData tenantData = tenantDataInfoDao.findByTenantId(tenantId).orElse(null);
        if (tenantData != null) {
            tenantData.setStatus("DISABLED");
            tenantData.setIsDel(true);
            tenantData.setUpdateTime(LocalDateTime.now());
            tenantDataInfoDao.saveAndFlush(tenantData);
        }
        registerAfterCommit(() -> {
            dataSource.removeDataSource(tenantId);
            changePublisher.publish(DataSourceChangeEvent.remove(tenantId));
        });
        return true;
    }

    @Override
    public DataSource activateTenantDataSource(Integer tenantId) {
        DataSource pool = ensureTenantDataSource(tenantId);
        if (pool == null) {
            throw new BaseException("租户数据源不可用，请检查开通状态或重试开通");
        }
        changePublisher.publish(DataSourceChangeEvent.add(tenantId));
        return pool;
    }

    @Override
    public DataSource ensureTenantDataSource(Integer tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return null;
        }
        DataSource existing = dataSource.getResolvedDataSources().get(tenantId);
        if (existing != null) {
            return existing;
        }
        Tenant tenant = tenantDao.findByIdAndIsDelFalse(tenantId).orElse(null);
        TenantData tenantData = tenantDataInfoDao.findByTenantId(tenantId).orElse(null);
        if (tenant == null || !"ACTIVE".equals(tenant.getStatus())
                || (tenant.getExpiresAt() != null && tenant.getExpiresAt().isBefore(LocalDateTime.now()))
                || tenantData == null || !"READY".equals(tenantData.getStatus())
                || Boolean.TRUE.equals(tenantData.getIsDel())) {
            return null;
        }
        DruidDataSource pool = tenantDataSourceFactory.create(tenantData);
        try (Connection ignored = pool.getConnection()) {
            if (dataSource.getResolvedDataSources().containsKey(tenantId)) {
                return dataSource.getResolvedDataSources().get(tenantId);
            }
            dataSource.addDataSources(tenantId, pool);
            return pool;
        } catch (Exception e) {
            pool.close();
            throw new BaseException("租户数据库连接失败");
        }
    }

    @Override
    @Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
    public boolean testTenantDataSource(Integer tenantId) {
        TenantData tenantData = getTenantData(tenantId).orElseThrow(() -> new BaseException("租户数据源不存在"));
        DruidDataSource pool = tenantDataSourceFactory.create(tenantData);
        try (Connection ignored = pool.getConnection()) {
            tenantData.setLastCheckAt(LocalDateTime.now());
            tenantDataInfoDao.save(tenantData);
            return true;
        } catch (Exception e) {
            throw new BaseException("租户数据库连接失败");
        } finally {
            pool.close();
        }
    }

    @Override
    public Map<String, Integer> getPoolStatistics() {
        int active = 0;
        int idle = 0;
        for (DataSource value : dataSource.getResolvedDataSources().values()) {
            if (value instanceof DruidDataSource druid) {
                active += druid.getActiveCount();
                idle += druid.getPoolingCount();
            }
        }
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("tenantPools", dataSource.getResolvedDataSources().size());
        stats.put("activeConnections", active);
        stats.put("idleConnections", idle);
        return stats;
    }

    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
