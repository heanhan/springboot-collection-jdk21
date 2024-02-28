package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.RightsUtils;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.service.MenuService;
import com.example.dynamic.jpa.system.service.RoleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoleServiceImpl implements RoleService {

    @Resource
    public MenuService menuService;

    @Resource
    public RoleDao roleDao;



    @Override
    public Role getAdminRole() {
        return null;
    }

    @Override
    public List<Menu> listMenusByRole(Integer roleId, Integer type) {
        Role role = getRoleById(roleId);
        if (role == null) {
            return Collections.emptyList();
        }
        //没有相关的权限数据
        if (StringUtils.isEmpty(role.getMenuRights())) {
            return Collections.emptyList();
        }
        List<Menu> menus = menuService.listAll();
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        // 筛选类型
        if (type != null) {
            menus = menus.stream().filter(item -> item.getType().equals(type)).collect(Collectors.toList());
        }
        // 如果是admin用户直接返回，不需要判断
        List<Menu> resultMenus = new ArrayList<>();
        // 判断是否拥有该权限
        for (Menu menu : menus) {
            if (RightsUtils.testRights(role.getMenuRights(), menu.getId())) {
                resultMenus.add(menu);
            }
        }
        return resultMenus;
    }

    @Override
    public List<Menu> listRoleMenusForTree(Integer roleId) {
        Validate.notNull(roleId, "角色id不能为空");
        Role role = getRoleById(roleId);
        if (role == null) {
            return Collections.emptyList();
        }
        if (StringUtils.isEmpty(role.getMenuRights())) {
            return Collections.emptyList();
        }
        // 获取所有的菜单
        List<Menu> menus = menuService.listByPidInTree(SystemConstant.ROOT_PARENT_ID);
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        // 筛选出当前角色的菜单
        hasMenuPermission(new BigInteger(role.getMenuRights()), menus);
        return menus;
    }

    @Override
    public List<AuthNode> listRoleAuthForTree(Integer roleId) {
        return null;
    }

    @Override
    public List<Role> listRoles() {
        return null;
    }

    @Override
    public boolean addRole(Role role) {
        return false;
    }

    @Override
    public boolean deleteRole(Integer roleId) {
        return false;
    }

    /**
     * 判断是否拥有这种权限
     */
    private void hasMenuPermission(BigInteger rights, List<Menu> list) {
        for (Menu menu : list) {
            if (!menu.getChildren().isEmpty()) {
                hasMenuPermission(rights, menu.getChildren());
            }
            if (RightsUtils.testRights(rights, menu.getId())) {
                menu.setChecked(true);
                return;
            }
            menu.setChecked(false);
        }
    }

    @Override
    public boolean deleteRoles(List<Integer> roleIds) {
        Validate.notEmpty(roleIds, "角色id列表不能为空");
        roleIds.forEach(item -> {
            boolean b = deleteRole(item);
            if (!b) {
                throw new BaseException("删除失败");
            }
        });
        return true;
    }

    @Override
    public boolean updateRole(Role role) {
        return false;
    }

    @Override
    public Role getRoleById(Integer roleId) {
        //todo 需要重写
        return roleDao.findById(roleId).get();
    }

}
