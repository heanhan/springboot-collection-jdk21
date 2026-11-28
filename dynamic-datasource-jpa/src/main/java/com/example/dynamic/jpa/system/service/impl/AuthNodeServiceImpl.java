package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.AuthNodeDao;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.dao.RolePermissionDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(value = "systemTransactionManager", rollbackFor = Exception.class)
public class AuthNodeServiceImpl implements AuthNodeService {

    @Resource
    private AuthNodeDao authNodeDao;

    @Resource
    private RoleDao roleDao;

    @Resource
    private RolePermissionDao rolePermissionDao;

    private volatile List<AuthNode> activeNodeCache;

    @Override
    public List<AuthNode> listActiveAuthNodes() {
        List<AuthNode> cache = activeNodeCache;
        if (cache == null) {
            cache = refreshActiveAuthNodes();
        }
        return cache;
    }

    @Override
    public List<AuthNode> refreshActiveAuthNodes() {
        return reloadActiveNodes();
    }

    @Override
    public List<AuthNode> listAuthNodesByRole(Integer roleId) {
        if (roleDao.findByIdAndIsDelFalse(roleId).isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> permissions = rolePermissionDao.findByRoleId(roleId).stream()
                .map(permission -> permission.getPermissionCode())
                .collect(java.util.stream.Collectors.toSet());
        return listActiveAuthNodes().stream()
                .filter(node -> permissions.contains(node.getPermissionCode()))
                .toList();
    }

    @Override
    public List<AuthNode> listAuthNodeTreeByRole(Integer roleId) {
        return buildTree(listAuthNodesByRole(roleId));
    }

    @Override
    public AuthNode addAuthNode(AuthNode authNode) {
        if (authNode == null) {
            throw new BaseException("权限节点不能为空");
        }
        normalize(authNode);
        authNode.setId(null);
        authNode.setIsDel(false);
        authNode.setCreateTime(new Date());
        if (existsSameEndpoint(authNode.getPath(), authNode.getMethod(), null)) {
            throw new BaseException("权限节点已存在");
        }
        AuthNode saved = authNodeDao.save(authNode);
        refreshActiveAuthNodes();
        return saved;
    }

    @Override
    public AuthNode editAuthNode(AuthNode authNode) {
        if (authNode == null) {
            throw new BaseException("权限节点不能为空");
        }
        AuthNode existing = getAuthNodeById(authNode.getId());
        if (existing == null) {
            throw new BaseException("权限节点不存在");
        }
        normalize(authNode);
        if (existsSameEndpoint(authNode.getPath(), authNode.getMethod(), existing.getId())) {
            throw new BaseException("权限节点已存在");
        }
        existing.setName(authNode.getName());
        existing.setPath(authNode.getPath());
        existing.setMethod(authNode.getMethod());
        existing.setPermissionCode(authNode.getPermissionCode());
        existing.setParentId(authNode.getParentId());
        existing.setListOrder(authNode.getListOrder());
        existing.setUpdateTime(new Date());
        AuthNode saved = authNodeDao.save(existing);
        refreshActiveAuthNodes();
        return saved;
    }

    @Override
    public boolean deleteAuthNode(Integer id) {
        AuthNode existing = getAuthNodeById(id);
        if (existing == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "权限节点不存在");
        }
        existing.setIsDel(true);
        existing.setUpdateTime(new Date());
        authNodeDao.save(existing);
        refreshActiveAuthNodes();
        return true;
    }

    @Override
    public AuthNode getAuthNodeById(Integer id) {
        return id == null ? null : authNodeDao.findByIdAndIsDelFalse(id).orElse(null);
    }

    private synchronized List<AuthNode> reloadActiveNodes() {
        List<AuthNode> nodes = authNodeDao.findAllByIsDelFalseOrderByIdAsc();
        nodes.forEach(node -> {
            if (StringUtils.isBlank(node.getPermissionCode())) {
                throw new BaseException("权限节点缺少权限编码: " + node.getPath());
            }
            node.setPermissionCode(node.getPermissionCode().trim().toUpperCase());
        });
        activeNodeCache = Collections.unmodifiableList(new ArrayList<>(nodes));
        return activeNodeCache;
    }

    private boolean existsSameEndpoint(String path, String method, Integer excludeId) {
        boolean exists = StringUtils.isBlank(method)
                ? authNodeDao.existsByPathAndMethodIsNullAndIsDelFalse(path)
                : authNodeDao.existsByPathAndMethodAndIsDelFalse(path, method);
        if (!exists || excludeId == null) {
            return exists;
        }
        return listActiveAuthNodes().stream()
                .anyMatch(node -> Objects.equals(node.getPath(), path)
                        && Objects.equals(node.getMethod(), method)
                        && !Objects.equals(node.getId(), excludeId));
    }

    private void normalize(AuthNode authNode) {
        if (StringUtils.isBlank(authNode.getPath())) {
            throw new BaseException("权限节点路径不能为空");
        }
        authNode.setPath(authNode.getPath().trim());
        authNode.setMethod(StringUtils.isBlank(authNode.getMethod())
                ? null : authNode.getMethod().trim().toUpperCase());
        if (StringUtils.isBlank(authNode.getPermissionCode())
                || !authNode.getPermissionCode().trim().matches("^[A-Z][A-Z0-9_:.-]{2,63}$")) {
            throw new BaseException("权限编码格式错误");
        }
        authNode.setPermissionCode(authNode.getPermissionCode().trim().toUpperCase());
        if (authNode.getParentId() == null) {
            authNode.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        if (authNode.getListOrder() == null) {
            authNode.setListOrder(1);
        }
    }

    private List<AuthNode> buildTree(List<AuthNode> nodes) {
        List<AuthNode> roots = new ArrayList<>();
        for (AuthNode node : nodes) {
            node.setChildren(new ArrayList<>());
            if (Objects.equals(node.getParentId(), SystemConstant.ROOT_PARENT_ID)) {
                roots.add(node);
            }
        }
        attachChildren(nodes, roots);
        return roots;
    }

    private void attachChildren(List<AuthNode> nodes, List<AuthNode> parents) {
        for (AuthNode parent : parents) {
            List<AuthNode> children = nodes.stream()
                    .filter(node -> Objects.equals(node.getParentId(), parent.getId()))
                    .toList();
            if (!children.isEmpty()) {
                parent.setChildren(new ArrayList<>(children));
                parent.setChecked(true);
                attachChildren(nodes, parent.getChildren());
            }
        }
    }
}
