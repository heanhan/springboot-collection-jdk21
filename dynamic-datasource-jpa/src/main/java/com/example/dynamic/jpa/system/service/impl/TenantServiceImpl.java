package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.service.TenantService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantDao tenantDao;

    @Override
    public Tenant addTenant(Tenant tenant) {
        if (tenant == null || StringUtils.isBlank(tenant.getTenantName())) {
            throw new BaseException("租户名称不能为空");
        }
        if (StringUtils.isBlank(tenant.getPhone())) {
            throw new BaseException("租户联系电话不能为空");
        }
        String tenantName = tenant.getTenantName().trim();
        if (getTenantByName(tenantName) != null) {
            throw new BaseException("租户名称已存在");
        }
        if (checkMobileExists(tenant.getPhone())) {
            throw new BaseException("电话号码已经被其他租户绑定");
        }
        String tenantCode = StringUtils.isBlank(tenant.getTenantCode())
                ? "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : tenant.getTenantCode().trim().toLowerCase();
        if (!tenantCode.matches("^[a-z][a-z0-9_]{2,31}$")) {
            throw new BaseException("租户编码须以字母开头，长度为3-32位");
        }
        if (tenantDao.existsByTenantCodeAndIsDelFalse(tenantCode)) {
            throw new BaseException("租户编码已存在");
        }
        tenant.setId(null);
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(tenantName);
        tenant.setStatus("PENDING");
        tenant.setProvisionStatus("PENDING");
        tenant.setExpiresAt(tenant.getExpiresAt() == null ? LocalDateTime.now().plusYears(1) : tenant.getExpiresAt());
        tenant.setSchemaVersion(0);
        tenant.setIsDel(false);
        tenant.setCreateTime(LocalDateTime.now());
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
        existing.setTenantName(tenant.getTenantName().trim());
        existing.setPhone(tenant.getPhone());
        existing.setEmail(tenant.getEmail());
        existing.setLogoUrl(tenant.getLogoUrl());
        existing.setUpdateTime(LocalDateTime.now());
        tenantDao.save(existing);
        return true;
    }

    @Override
    public boolean deleteTenant(Integer id) {
        Tenant tenant = getTenantById(id);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        if ("DESTROYED".equals(tenant.getStatus())) {
            return true;
        }
        throw new BaseException("租户删除必须通过销毁流程执行");
    }

    @Override
    public Tenant getTenantById(Integer id) {
        return id == null ? null : tenantDao.findByIdAndIsDelFalse(id).orElse(null);
    }

    @Override
    public List<Tenant> listAllTenant() {
        return tenantDao.findAllByIsDelFalseOrderByIdDesc();
    }

    @Override
    public Tenant getTenantByName(String tenantName) {
        return StringUtils.isBlank(tenantName) ? null
                : tenantDao.findByTenantNameAndIsDelFalse(tenantName.trim()).orElse(null);
    }

    @Override
    public Tenant getTenantByCode(String tenantCode) {
        return StringUtils.isBlank(tenantCode) ? null
                : tenantDao.findByTenantCodeAndIsDelFalse(tenantCode.trim().toLowerCase()).orElse(null);
    }

    @Override
    public boolean checkMobileExists(String mobile) {
        if (StringUtils.isBlank(mobile)) {
            throw new BaseException("手机号不能为空");
        }
        return tenantDao.existsByPhoneAndIsDelFalse(mobile.trim());
    }
}
