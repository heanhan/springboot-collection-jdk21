package com.example.dynamic.jpa.system.service.impl;


import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.enums.UserStatus;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 系統用戶表 服务实现类
 * </p>
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    @Resource
    private RoleService roleService;

    @Resource
    private TenantService tenantService;

    @Resource
    private UserDao userDao;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;


    @Override
    public List<Menu> listUserMenus(Integer userId, Integer type) {
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException("获取用户菜单错误");
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null) {
            return Collections.emptyList();
        }
        return roleService.listMenusByRole(role.getId(), type);
    }

    @Override
    public List<Menu> listUserMenusForTree(Integer userId, Integer type) {
        List<Menu> menus = listUserMenus(userId, type);
        if (!menus.isEmpty()) {
            menus = menus.stream().filter(item -> item.getType().equals(type)).
                    collect(Collectors.toList());
            List<Menu> menuTree = new ArrayList<>();
            for (Menu menu : menus) {
                if (menu.getParentId() == SystemConstant.ROOT_PARENT_ID) {
                    menuTree.add(menu);
                }
            }
            parseTreeMenus(menus, menuTree);
            return menuTree;
        }
        return Collections.emptyList();
    }


    /**
     * 将普通的列表对象解析成tree格式
     */
    private void parseTreeAuthNodes(List<AuthNode> authNodes, List<AuthNode> authNodeTree) {
        for (AuthNode parentNode : authNodeTree) {
            List<AuthNode> childrenNodes = new ArrayList<>();
            for (AuthNode authNode : authNodes) {
                if (Objects.equals(authNode.getParentId(), parentNode.getId())) {
                    childrenNodes.add(authNode);
                }
            }
            if (!childrenNodes.isEmpty()) {
                parentNode.setChecked(true);
                parentNode.setChildren(childrenNodes);
                parseTreeAuthNodes(authNodes, parentNode.getChildren());
            }
        }
    }

    /**
     * 将普通的列表对象解析成tree格式
     *
     * @param menus    所有的菜单
     * @param menuTree 填充之后的树形菜单
     */
    private void parseTreeMenus(List<Menu> menus, List<Menu> menuTree) {
        for (Menu parentMenu : menuTree) {
            List<Menu> childrenMenus = new ArrayList<>();
            for (Menu menu : menus) {
                if (Objects.equals(menu.getParentId(), parentMenu.getId())) {
                    childrenMenus.add(menu);
                }
            }
            if (!childrenMenus.isEmpty()) {
                parentMenu.setChecked(true);
                parentMenu.setChildren(childrenMenus);
                parseTreeMenus(menus, parentMenu.getChildren());
            }
        }
    }

    @Override
    public User addUser(User user) {
        if (user == null) {
            throw new BaseException("用户不能为空");
        }
        if (StringUtils.isBlank(user.getUsername()) || !user.getUsername().matches("^[a-zA-Z0-9_@.]{5,16}$")) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "用户名格式错误");
        }
        if (StringUtils.isBlank(user.getPassword()) || user.getPassword().length() < 6) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "密码长度不能少于6位");
        }
        if (user.getTenantId() == null || user.getTenantId() < 0) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "租户id错误");
        }
        if (user.getTenantId() > 0 && tenantService.getTenantById(user.getTenantId()) == null) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "租户不存在");
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "角色不存在");
        }
        if (userDao.existsByUsername(user.getUsername())) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "用户名已存在");
        }
        if (StringUtils.isNotBlank(user.getPhone())
                && userDao.existsByPhoneAndStatus(user.getPhone(), UserStatus.NORMAL.getValue())) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "手机号码已存在");
        }
        user.setId(null);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.NORMAL.getValue());
        }
        user.setCreateTime(java.time.LocalDateTime.now());
        return userDao.save(user);
    }

    @Override
    public User editUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "用户id不能为空");
        }
        User existing = getUserById(user.getId());
        if (existing == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "用户不存在");
        }
        if (StringUtils.isNotBlank(user.getPhone())
                && checkMobileExists(user.getPhone(), existing.getId())) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "手机号码已存在");
        }
        if (user.getRoleId() != null && !user.getRoleId().equals(existing.getRoleId())) {
            Role role = roleService.getRoleById(user.getRoleId());
            if (role == null) {
                throw new BaseException(ExceptionCode.EDIT.getCode(), "修改的角色不存在");
            }
            existing.setRoleId(user.getRoleId());
        }
        if (user.getTenantId() != null && !user.getTenantId().equals(existing.getTenantId())) {
            if (user.getTenantId() < 0
                    || (user.getTenantId() > 0 && tenantService.getTenantById(user.getTenantId()) == null)) {
                throw new BaseException(ExceptionCode.EDIT.getCode(), "租户不存在");
            }
            existing.setTenantId(user.getTenantId());
        }
        existing.setNickname(user.getNickname());
        existing.setEmail(user.getEmail());
        if (StringUtils.isNotBlank(user.getPhone())) {
            existing.setPhone(user.getPhone());
        }
        existing.setAvatar(user.getAvatar());
        existing.setRemark(user.getRemark());
        existing.setType(user.getType());
        if (user.getStatus() != null) {
            existing.setStatus(user.getStatus());
        }
        existing.setUpdateTime(java.time.LocalDateTime.now());
        return userDao.save(existing);
    }


    @Override
    public void deleteUser(Integer userId) {
        if (userId == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户id不能为空");
        }
        User user = getUserById(userId);
        if (user == null || Objects.equals(user.getStatus(), UserStatus.DELETED.getValue())) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户不存在");
        }
        user.setStatus(UserStatus.DELETED.getValue());
        user.setUpdateTime(java.time.LocalDateTime.now());
        userDao.save(user);
    }


    @Override
    public boolean batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "id不能为空");
        }
        ids.forEach(this::deleteUser);
        return true;
    }

    @Override
    public User getUserById(Integer userId) {
        return userId == null ? null : userDao.findById(userId).orElse(null);
    }

    @Override
    public User getUserByName(String username) {
        User user= userDao.getUserByName(username,UserStatus.NORMAL.getValue());
        return user;
    }

    @Override
    public boolean checkMobileExists(String mobile, Integer userId) {
        if (StringUtils.isBlank(mobile)) {
            return false;
        }
        return userId == null
                ? userDao.existsByPhoneAndStatus(mobile, UserStatus.NORMAL.getValue())
                : userDao.existsByPhoneAndIdNotAndStatus(mobile, userId, UserStatus.NORMAL.getValue());
    }

    @Override
    public boolean editUserPassword(Integer userId, String oldPassword, String newPassword) {
        if (StringUtils.isEmpty(oldPassword) || StringUtils.isEmpty(newPassword)) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "密码不能为空");
        }
        User user = this.getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "用户不存在");
        }
        boolean validPassword = validUserPassword(user.getId(), oldPassword);
        if (!validPassword) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "旧密码校验失败");
        }
        if (newPassword.length() < 6) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "密码长度要大于6位");
        }
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        return true;
    }

    @Override
    public boolean validUserPassword(Integer userId, String password) {
        User user = this.getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户不存在");
        }
        return bCryptPasswordEncoder.matches(password, user.getPassword());
    }

    @Override
    public boolean resetPassword(Integer userId) {
        User user = this.getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户不存在");
        }
        user.setPassword(bCryptPasswordEncoder.encode("123456"));
        return true;
    }

}
