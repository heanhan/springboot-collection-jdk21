package com.example.dynamic.jpa.system.service;


import com.example.dynamic.jpa.system.entity.Tenant;

import java.util.List;

/**
 * <p>
 * saas租户 服务类
 * </p>
 *
 * @author zhaojh
 */
public interface TenantService {

    /**
     * 添加租户
     *
     * @param tenant 租户信息
     */
    Tenant addTenant(Tenant tenant);

    /**
     * 编辑租户信息
     *
     * @param tenant 租户信息
     * @return boolean
     */
    boolean editTenant(Tenant tenant);

    /**
     * 删除租户
     *
     * @param id 租户id
     * @return boolean
     */
    boolean deleteTenant(String id);

    /**
     * 根据租户id获取租户
     *
     * @param id 租户id
     * @return cn.greenbon.api.business.system.bean.Tenant
     */
    Tenant getTenantById(String id);

    /**
     * 获取所有租户
     *
     * @return java.util.List<cn.greenbon.api.business.system.bean.Tenant>
     */
    List<Tenant> listAllTenant();


    /**
     * 根据租户名称获取租户
     *
     * @param tenantName 租户名称
     * @return cn.greenbon.api.business.system.bean.Tenant
     */
    Tenant getTenantByName(String tenantName);

    /**
     * 校验租户电话号码是否被绑定
     *
     * @param mobile   电话号码
     * @return boolean
     */
    boolean checkMobileExists(String mobile);
}
