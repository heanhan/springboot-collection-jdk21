package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;

import java.util.List;

public interface RoleService {

    List<Menu> listMenusByRole(Integer roleId, Integer type);

    List<Menu> listRoleMenusForTree(Integer roleId);

    List<AuthNode> listRoleAuthForTree(Integer roleId);

    List<Role> listRoles(Integer tenantId);

    List<String> listRolePermissions(Integer roleId);

    void replaceRolePermissions(Integer roleId, List<String> permissionCodes);

    boolean addRole(Role role);

    boolean deleteRole(Integer roleId);

    boolean deleteRoles(List<Integer> roleIds);

    boolean updateRole(Role role);

    Role getRoleById(Integer roleId);
}
