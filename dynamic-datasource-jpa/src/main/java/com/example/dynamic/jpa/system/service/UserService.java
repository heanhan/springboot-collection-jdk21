package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.User;

import java.util.List;

public interface UserService {

    List<Menu> listUserMenus(Integer userId, Integer type);

    List<Menu> listUserMenusForTree(Integer userId, Integer type);

    User addUser(User user);

    User editUser(User user);

    void deleteUser(Integer userId);

    boolean batchDelete(List<Integer> ids);

    User getUserById(Integer userId);

    User getUserByName(String username, Integer tenantId);

    List<User> listUsers(Integer tenantId);

    List<Role> listUserRoles(Integer userId);

    boolean checkMobileExists(String mobile, Integer tenantId, Integer userId);

    boolean editUserPassword(Integer userId, String oldPassword, String newPassword);

    boolean validUserPassword(Integer userId, String password);

    boolean resetPassword(Integer userId, String newPassword);

    void assignRoles(Integer userId, List<Integer> roleIds);

    void revokeTenantSessions(Integer tenantId);

    void destroyTenantUsers(Integer tenantId);
}
