package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.service.TenantDataInfoService;
import com.example.dynamic.jpa.system.service.TenantService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * saas租户 服务实现类
 * </p>
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantDao tenantDao;

    @Resource
    private TenantDataInfoService tenantDataInfoService;

    @Override
    public Tenant addTenant(Tenant tenant) {
        if (tenant == null || StringUtils.isBlank(tenant.getTenantName())) {
            throw new BaseException("租户名称不能为空");
        }
        tenant.setId(null);
        tenant.setIsDel(false);
        tenant.setCreateTime(java.time.LocalDateTime.now());
        Tenant tenantByName = getTenantByName(tenant.getTenantName());
        if (tenantByName != null) {
            throw new BaseException("租户名称已存在");
        }
        if (checkMobileExists(tenant.getPhone())) {
            throw new BaseException("电话号码已经被其他租户绑定");
        }
        return tenantDao.save(tenant);
    }

    @Override
    public boolean editTenant(Tenant tenant) {
        Tenant existing = tenant == null ? null : getTenantById(tenant.getId());
        if (existing == null) {
            throw new BaseException("租户不存在");
        }
        if (StringUtils.isBlank(tenant.getTenantName()) || StringUtils.isBlank(tenant.getPhone())) {
            throw new BaseException("租户名称和手机号不能为空");
        }
        Tenant sameName = getTenantByName(tenant.getTenantName());
        if (sameName != null && !sameName.getId().equals(existing.getId())) {
            throw new BaseException("租户名称已存在");
        }
        if (tenantDao.existsByPhoneAndIdNotAndIsDelFalse(tenant.getPhone(), existing.getId())) {
            throw new BaseException("电话号码已经被其他租户绑定");
        }
        existing.setTenantName(tenant.getTenantName());
        existing.setPhone(tenant.getPhone());
        existing.setEmail(tenant.getEmail());
        existing.setLogoUrl(tenant.getLogoUrl());
        existing.setUpdateTime(java.time.LocalDateTime.now());
        tenantDao.save(existing);
        return true;
    }

    @Override
    public boolean deleteTenant(Integer id) {
        Tenant tenant = getTenantById(id);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        tenant.setIsDel(true);
        tenant.setUpdateTime(java.time.LocalDateTime.now());
        tenantDao.save(tenant);
        if (id > 0) {
            tenantDataInfoService.disableTenantDataSource(id);
        }
        return true;
    }

    @Override
    public Tenant getTenantById(Integer id) {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getId, id);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return id == null ? null : tenantDao.findByIdAndIsDelFalse(id).orElse(null);
    }

    @Override
    public List<Tenant> listAllTenant() {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return tenantDao.findAllByIsDelFalse();
    }


    @Override
    public Tenant getTenantByName(String tenantName) {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getTenantName, tenantName);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return tenantDao.findByTenantNameAndIsDelFalse(tenantName).orElse(null);
    }

    @Override
    public boolean checkMobileExists(String mobile) {
        if (StringUtils.isBlank(mobile)) {
            throw new BaseException(ExceptionCode.OPERATE.getCode(), "手机号不能为空");
        }
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getPhone, mobile);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return tenantDao.existsByPhoneAndIsDelFalse(mobile);
    }
}
