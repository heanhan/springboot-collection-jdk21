package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.RightsUtils;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import com.example.dynamic.jpa.system.service.MenuService;
import com.example.dynamic.jpa.system.service.RoleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
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

    @Resource
    private UserDao userDao;

    @Resource
    private AuthNodeService authNodeService;



    @Override
    public Role getAdminRole() {
        return roleDao.findByRoleNameAndIsDelFalse(SystemConstant.ROLE_TENANT_ADMIN).orElse(null);
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
        return authNodeService.listAuthNodeTreeByRole(roleId);
    }

    @Override
    public List<Role> listRoles() {
        return roleDao.findAllByIsDelFalseOrderByIdAsc();
    }

    @Override
    public boolean addRole(Role role) {
        Validate.notNull(role, "角色不能为空");
        if (StringUtils.isBlank(role.getRoleName())) {
            throw new BaseException("角色名称不能为空");
        }
        role.setId(null);
        role.setRoleName(role.getRoleName().trim());
        if (roleDao.existsByRoleNameAndIsDelFalse(role.getRoleName())) {
            throw new BaseException("角色名称已存在");
        }
        if (role.getParentId() == null) {
            role.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        role.setIsDel(false);
        role.setCreateTime(LocalDateTime.now());
        roleDao.save(role);
        return true;
    }

    @Override
    public boolean deleteRole(Integer roleId) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "角色不存在");
        }
        if (userDao.existsByRoleIdAndStatus(roleId, com.example.dynamic.jpa.common.enums.UserStatus.NORMAL.getValue())) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "角色下仍有正常用户，不能删除");
        }
        role.setIsDel(true);
        role.setUpdateTime(LocalDateTime.now());
        roleDao.save(role);
        return true;
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
        Validate.notNull(role, "角色不能为空");
        Role existing = getRoleById(role.getId());
        if (existing == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "角色不存在");
        }
        if (StringUtils.isNotBlank(role.getRoleName())) {
            String roleName = role.getRoleName().trim();
            if (roleDao.existsByRoleNameAndIdNotAndIsDelFalse(roleName, existing.getId())) {
                throw new BaseException(ExceptionCode.EDIT.getCode(), "角色名称已存在");
            }
            existing.setRoleName(roleName);
        }
        existing.setDescription(role.getDescription());
        existing.setMenuRights(role.getMenuRights());
        existing.setNodeRights(role.getNodeRights());
        existing.setParentId(role.getParentId() == null ? existing.getParentId() : role.getParentId());
        existing.setType(role.getType());
        existing.setUpdateTime(LocalDateTime.now());
        roleDao.save(existing);
        return true;
    }

    @Override
    public Role getRoleById(Integer roleId) {
        return roleId == null ? null : roleDao.findByIdAndIsDelFalse(roleId).orElse(null);
    }

}
