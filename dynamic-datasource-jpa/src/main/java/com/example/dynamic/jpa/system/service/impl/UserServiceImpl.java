package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.enums.UserStatus;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.security.token.TokenStore;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.UserDao;
import com.example.dynamic.jpa.system.dao.UserRoleDao;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.User;
import com.example.dynamic.jpa.system.entity.UserRole;
import com.example.dynamic.jpa.system.service.RoleService;
import com.example.dynamic.jpa.system.service.TenantService;
import com.example.dynamic.jpa.system.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    @Resource
    private RoleService roleService;

    @Resource
    private TenantService tenantService;

    @Resource
    private UserDao userDao;

    @Resource
    private UserRoleDao userRoleDao;

    @Resource
    private RoleDao roleDao;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Resource
    private TokenStore tokenStore;

    @Override
    public List<Menu> listUserMenus(Integer userId, Integer type) {
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException("获取用户菜单错误");
        }
        List<Role> roles = listUserRoles(userId);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Menu> menus = new ArrayList<>();
        for (Role role : roles) {
            menus.addAll(roleService.listMenusByRole(role.getId(), type));
        }
        return menus.stream().distinct().toList();
    }

    @Override
    public List<Menu> listUserMenusForTree(Integer userId, Integer type) {
        List<Menu> menus = listUserMenus(userId, type);
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        menus = menus.stream().filter(item -> Objects.equals(item.getType(), type)).toList();
        List<Menu> menuTree = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == SystemConstant.ROOT_PARENT_ID) {
                menuTree.add(menu);
            }
        }
        parseTreeMenus(menus, menuTree);
        return menuTree;
    }

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
        if (user.getTenantId() > 0) {
            var tenant = tenantService.getTenantById(user.getTenantId());
            if (tenant == null || !("ACTIVE".equals(tenant.getStatus()) || "PENDING".equals(tenant.getStatus()))
                    || Boolean.TRUE.equals(tenant.getIsDel())) {
                throw new BaseException(ExceptionCode.INSERT.getCode(), "租户不存在或已停用");
            }
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null || !Objects.equals(role.getTenantId(), user.getTenantId())) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "角色不存在或不属于当前租户");
        }
        if (userDao.existsByUsernameAndTenantId(user.getUsername(), user.getTenantId())) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "用户名已存在");
        }
        if (StringUtils.isNotBlank(user.getPhone())
                && userDao.existsByPhoneAndTenantIdAndStatus(user.getPhone(), user.getTenantId(), UserStatus.NORMAL.getValue())) {
            throw new BaseException(ExceptionCode.INSERT.getCode(), "手机号码已存在");
        }
        user.setId(null);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.NORMAL.getValue());
        }
        user.setTokenVersion(0);
        user.setCreateTime(LocalDateTime.now());
        User saved = userDao.save(user);
        replaceUserRoles(saved.getId(), List.of(role.getId()));
        return saved;
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
                && checkMobileExists(user.getPhone(), existing.getTenantId(), existing.getId())) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "手机号码已存在");
        }
        existing.setNickname(user.getNickname());
        existing.setEmail(user.getEmail());
        if (StringUtils.isNotBlank(user.getPhone())) {
            existing.setPhone(user.getPhone());
        }
        existing.setAvatar(user.getAvatar());
        existing.setRemark(user.getRemark());
        existing.setType(user.getType());
        if (user.getStatus() != null && !Objects.equals(user.getStatus(), existing.getStatus())) {
            existing.setStatus(user.getStatus());
            invalidateUserTokens(existing);
        }
        existing.setUpdateTime(LocalDateTime.now());
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
        user.setUpdateTime(LocalDateTime.now());
        userDao.save(user);
        invalidateUserTokens(user);
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
    public User getUserByName(String username, Integer tenantId) {
        return userDao.findFirstByUsernameAndTenantIdAndStatus(
                username, tenantId, UserStatus.NORMAL.getValue()).orElse(null);
    }

    @Override
    public List<User> listUsers(Integer tenantId) {
        List<User> users = userDao.findAllByTenantIdAndStatusOrderByIdAsc(tenantId, UserStatus.NORMAL.getValue());
        if (users.isEmpty()) {
            return users;
        }
        Map<Integer, List<Integer>> roleIdsByUser = userRoleDao.findByUserIdIn(users.stream().map(User::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        users.forEach(user -> user.setRoleIds(roleIdsByUser.getOrDefault(user.getId(),
                user.getRoleId() == null ? Collections.emptyList() : List.of(user.getRoleId()))));
        return users;
    }

    @Override
    public List<Role> listUserRoles(Integer userId) {
        List<Integer> roleIds = userRoleDao.findByUserId(userId).stream()
                .map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            User user = getUserById(userId);
            if (user != null && user.getRoleId() != null) {
                roleIds = List.of(user.getRoleId());
            }
        }
        return roleIds.stream()
                .map(roleService::getRoleById)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean checkMobileExists(String mobile, Integer tenantId, Integer userId) {
        if (StringUtils.isBlank(mobile)) {
            return false;
        }
        return userId == null
                ? userDao.existsByPhoneAndTenantIdAndStatus(mobile, tenantId, UserStatus.NORMAL.getValue())
                : userDao.existsByPhoneAndTenantIdAndIdNotAndStatus(mobile, tenantId, userId, UserStatus.NORMAL.getValue());
    }

    @Override
    public boolean editUserPassword(Integer userId, String oldPassword, String newPassword) {
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "密码不能为空");
        }
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "用户不存在");
        }
        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "旧密码校验失败");
        }
        if (newPassword.length() < 6) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "密码长度不能少于6位");
        }
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userDao.save(user);
        invalidateUserTokens(user);
        return true;
    }

    @Override
    public boolean validUserPassword(Integer userId, String password) {
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户不存在");
        }
        return bCryptPasswordEncoder.matches(password, user.getPassword());
    }

    @Override
    public boolean resetPassword(Integer userId, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "新密码长度不能少于6位");
        }
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "用户不存在");
        }
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userDao.save(user);
        invalidateUserTokens(user);
        return true;
    }

    @Override
    public void assignRoles(Integer userId, List<Integer> roleIds) {
        User user = getUserById(userId);
        if (user == null) {
            throw new BaseException("用户不存在");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BaseException("至少分配一个角色");
        }
        for (Integer roleId : roleIds) {
            Role role = roleDao.findByIdAndIsDelFalse(roleId).orElse(null);
            if (role == null || !Objects.equals(role.getTenantId(), user.getTenantId())) {
                throw new BaseException("角色不存在或不属于当前租户");
            }
        }
        replaceUserRoles(userId, roleIds);
        user.setRoleId(roleIds.get(0));
        user.setUpdateTime(LocalDateTime.now());
        userDao.save(user);
        invalidateUserTokens(user);
    }

    @Override
    public void revokeTenantSessions(Integer tenantId) {
        for (User user : userDao.findAllByTenantId(tenantId)) {
            if (!Objects.equals(user.getStatus(), UserStatus.DELETED.getValue())) {
                invalidateUserTokens(user);
            }
        }
    }

    @Override
    public void destroyTenantUsers(Integer tenantId) {
        for (User user : userDao.findAllByTenantId(tenantId)) {
            user.setStatus(UserStatus.DELETED.getValue());
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            user.setUpdateTime(LocalDateTime.now());
            userDao.save(user);
            tokenStore.deleteAllRefreshTokens(String.valueOf(user.getId()));
        }
    }

    private void replaceUserRoles(Integer userId, List<Integer> roleIds) {
        userRoleDao.deleteByUserId(userId);
        userRoleDao.flush();
        for (Integer roleId : roleIds.stream().distinct().toList()) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleDao.save(userRole);
        }
    }

    private void invalidateUserTokens(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userDao.save(user);
        tokenStore.deleteAllRefreshTokens(String.valueOf(user.getId()));
    }
}
