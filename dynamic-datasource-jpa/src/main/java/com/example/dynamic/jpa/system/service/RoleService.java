package com.example.dynamic.jpa.system.service;


import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author zhaojh
 */
public interface RoleService {


    /**
     * 获取管理员的权限
     *
     * @return cn.greenbon.api.business.system.bean.Role
     */
    Role getAdminRole();

    /**
     * 获取角色所有的菜单权限
     *
     * @param roleId 角色id
     * @return java.util.List<cn.greenbon.api.business.system.bean.Menu>
     */
    List<Menu> listMenusByRole(Integer roleId, Integer type);

    /**
     * 获取角色菜单权限，按树形结构显示
     *
     * @param roleId 角色id
     * @return java.util.List<cn.greenbon.api.business.system.bean.Menu>
     */
    List<Menu> listRoleMenusForTree(Integer roleId);

    /**
     * 获取角色所拥有的树型节点权限
     *
     * @param roleId 角色id
     * @return java.util.List<cn.greenbon.api.business.system.bean.AuthNode>
     */
    List<AuthNode> listRoleAuthForTree(Integer roleId);

    /**
     * 获取所有的角色信息
     *
     * @return java.util.List<cn.greenbon.api.business.system.bean.Role>
     */
    List<Role> listRoles();

    /**
     * 添加角色
     *
     * @param role 角色对象
     * @return boolean
     */
    boolean addRole(Role role);

    /**
     * 删除角色
     *
     * @param roleId 角色id
     * @return boolean
     */
    boolean deleteRole(Integer roleId);

    /**
     * 批量删除角色
     *
     * @param roleIds 角色ids
     * @return boolean
     */
    boolean deleteRoles(List<Integer> roleIds);

    /**
     * 更新角色信息
     *
     * @param role 角色对象
     * @return boolean
     */
    boolean updateRole(Role role);

    /**
     * 根据id获取角色
     *
     * @param roleId 角色id
     * @return cn.greenbon.api.business.system.bean.Role
     */
    Role getRoleById(Integer roleId);

}
