package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.TenantDao;
import com.example.dynamic.jpa.system.entity.Tenant;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
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
    private RoleService roleService;

    @Resource
    private TenantDao tenantDao;

    @Resource
    private UserService userService;

    @Override
    public Tenant addTenant(Tenant tenant) {
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
        tenant.setIsDel(null);
        return true;
    }

    @Override
    public boolean deleteTenant(String id) {
        Tenant tenant = getTenantById(id);
        if (tenant == null) {
            throw new BaseException("租户不存在");
        }
        tenant.setIsDel(true);
        return true;
    }

    @Override
    public Tenant getTenantById(String id) {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getId, id);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return null;
    }

    @Override
    public List<Tenant> listAllTenant() {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return null;
    }


    @Override
    public Tenant getTenantByName(String tenantName) {
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getTenantName, tenantName);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return null;
    }

    @Override
    public boolean checkMobileExists(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
            throw new BaseException(ExceptionCode.OPERATE.getCode(), "手机号不能为空");
        }
//        QueryWrapper<Tenant> queryWrapper = new QueryWrapper<>();
//        queryWrapper.lambda().eq(Tenant::getPhone, mobile);
//        queryWrapper.lambda().eq(Tenant::getIsDel, false);
        return true;
    }
}
