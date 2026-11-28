package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.RightsUtils;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.RolePermissionDao;
import com.example.dynamic.jpa.system.dao.UserRoleDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.entity.RolePermission;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import com.example.dynamic.jpa.system.service.MenuService;
import com.example.dynamic.jpa.system.service.RoleService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
public class RoleServiceImpl implements RoleService {

    @Resource
    private MenuService menuService;

    @Resource
    private RoleDao roleDao;

    @Resource
    private UserRoleDao userRoleDao;

    @Resource
    private RolePermissionDao rolePermissionDao;

    @Resource
    private AuthNodeService authNodeService;

    @Override
    public List<Menu> listMenusByRole(Integer roleId, Integer type) {
        Role role = getRoleById(roleId);
        if (role == null || StringUtils.isEmpty(role.getMenuRights())) {
            return Collections.emptyList();
        }
        List<Menu> menus = menuService.listAll();
        if (type != null) {
            menus = menus.stream().filter(item -> Objects.equals(item.getType(), type)).collect(Collectors.toList());
        }
        return menus.stream()
                .filter(menu -> RightsUtils.testRights(role.getMenuRights(), menu.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> listRoleMenusForTree(Integer roleId) {
        Role role = getRoleById(roleId);
        if (role == null || StringUtils.isEmpty(role.getMenuRights())) {
            return Collections.emptyList();
        }
        List<Menu> menus = menuService.listByPidInTree(SystemConstant.ROOT_PARENT_ID);
        hasMenuPermission(new BigInteger(role.getMenuRights()), menus);
        return menus;
    }

    @Override
    public List<AuthNode> listRoleAuthForTree(Integer roleId) {
        return authNodeService.listAuthNodeTreeByRole(roleId);
    }

    @Override
    public List<Role> listRoles(Integer tenantId) {
        return roleDao.findAllByTenantIdAndIsDelFalseOrderByIdAsc(tenantId).stream()
                .peek(role -> role.setPermissionCodes(listRolePermissions(role.getId())))
                .toList();
    }

    @Override
    public List<String> listRolePermissions(Integer roleId) {
        return rolePermissionDao.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionCode)
                .distinct().sorted().toList();
    }

    @Override
    public void replaceRolePermissions(Integer roleId, List<String> permissionCodes) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new BaseException("角色不存在");
        }
        Set<String> activeCodes = authNodeService.listActiveAuthNodes().stream()
                .map(AuthNode::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> requested = permissionCodes == null ? Collections.emptySet() : new HashSet<>(permissionCodes);
        if (SystemConstant.ROLE_TENANT_ADMIN.equals(role.getRoleCode())) {
            Set<String> current = listRolePermissions(roleId).stream().collect(Collectors.toSet());
            if (!current.isEmpty() && !current.equals(requested)) {
                throw new BaseException("不能修改内置管理员角色权限");
            }
        }
        if (!activeCodes.containsAll(requested)) {
            throw new BaseException("存在无效的权限编码");
        }
        rolePermissionDao.deleteByRoleId(roleId);
        rolePermissionDao.flush();
        for (String code : requested) {
            RolePermission permission = new RolePermission();
            permission.setRoleId(roleId);
            permission.setPermissionCode(code);
            permission.setCreateTime(LocalDateTime.now());
            rolePermissionDao.save(permission);
        }
        role.setUpdateTime(LocalDateTime.now());
        roleDao.save(role);
    }

    @Override
    public boolean addRole(Role role) {
        if (role == null || StringUtils.isBlank(role.getRoleName())) {
            throw new BaseException("角色名称不能为空");
        }
        if (role.getTenantId() == null || role.getTenantId() < 0) {
            throw new BaseException("租户id错误");
        }
        String roleName = role.getRoleName().trim();
        if (roleDao.findAllByTenantIdAndIsDelFalseOrderByIdAsc(role.getTenantId()).stream()
                .anyMatch(item -> Objects.equals(item.getRoleName(), roleName))) {
            throw new BaseException("角色名称已存在");
        }
        String roleCode = StringUtils.isBlank(role.getRoleCode())
                ? "ROLE_CUSTOM_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : role.getRoleCode().trim().toUpperCase();
        if (roleDao.existsByRoleCodeAndTenantIdAndIsDelFalse(roleCode, role.getTenantId())) {
            throw new BaseException("角色编码已存在");
        }
        role.setId(null);
        role.setTenantId(role.getTenantId());
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        if (role.getParentId() == null) {
            role.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        if (role.getMenuRights() == null) {
            role.setMenuRights("0");
        }
        role.setIsDel(false);
        role.setCreateTime(LocalDateTime.now());
        Role saved = roleDao.save(role);
        replaceRolePermissions(saved.getId(), role.getPermissionCodes());
        return true;
    }

    @Override
    public boolean deleteRole(Integer roleId) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "角色不存在");
        }
        if (SystemConstant.ROLE_TENANT_ADMIN.equals(role.getRoleCode())) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "内置管理员角色不能删除");
        }
        if (userRoleDao.countByRoleId(roleId) > 0) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "角色下仍有用户，不能删除");
        }
        role.setIsDel(true);
        role.setUpdateTime(LocalDateTime.now());
        roleDao.save(role);
        rolePermissionDao.deleteByRoleId(roleId);
        return true;
    }

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
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BaseException("角色id列表不能为空");
        }
        roleIds.forEach(this::deleteRole);
        return true;
    }

    @Override
    public boolean updateRole(Role role) {
        if (role == null || role.getId() == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "角色id不能为空");
        }
        Role existing = getRoleById(role.getId());
        if (existing == null) {
            throw new BaseException(ExceptionCode.EDIT.getCode(), "角色不存在");
        }
        if (StringUtils.isNotBlank(role.getRoleName())) {
            String roleName = role.getRoleName().trim();
            if (roleDao.findAllByTenantIdAndIsDelFalseOrderByIdAsc(existing.getTenantId()).stream()
                    .anyMatch(item -> !Objects.equals(item.getId(), existing.getId())
                            && Objects.equals(item.getRoleName(), roleName))) {
                throw new BaseException(ExceptionCode.EDIT.getCode(), "角色名称已存在");
            }
            existing.setRoleName(roleName);
        }
        if (SystemConstant.ROLE_TENANT_ADMIN.equals(existing.getRoleCode())
                && role.getPermissionCodes() != null) {
            Set<String> current = new HashSet<>(listRolePermissions(existing.getId()));
            if (!current.equals(new HashSet<>(role.getPermissionCodes()))) {
                throw new BaseException(ExceptionCode.EDIT.getCode(), "不能修改内置管理员角色权限");
            }
        }
        existing.setDescription(role.getDescription());
        existing.setMenuRights(role.getMenuRights() == null ? existing.getMenuRights() : role.getMenuRights());
        existing.setNodeRights(role.getNodeRights() == null ? existing.getNodeRights() : role.getNodeRights());
        existing.setParentId(role.getParentId() == null ? existing.getParentId() : role.getParentId());
        existing.setType(role.getType());
        existing.setUpdateTime(LocalDateTime.now());
        roleDao.save(existing);
        if (role.getPermissionCodes() != null) {
            replaceRolePermissions(existing.getId(), role.getPermissionCodes());
        }
        return true;
    }

    @Override
    public Role getRoleById(Integer roleId) {
        return roleId == null ? null : roleDao.findByIdAndIsDelFalse(roleId).orElse(null);
    }
}
