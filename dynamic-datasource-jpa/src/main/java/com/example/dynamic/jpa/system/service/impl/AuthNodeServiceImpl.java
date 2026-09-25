package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.common.util.RightsUtils;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.AuthNodeDao;
import com.example.dynamic.jpa.system.dao.RoleDao;
import com.example.dynamic.jpa.system.entity.AuthNode;
import com.example.dynamic.jpa.system.entity.Role;
import com.example.dynamic.jpa.system.service.AuthNodeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 权限节点服务实现
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AuthNodeServiceImpl implements AuthNodeService {

    @Resource
    private AuthNodeDao authNodeDao;

    @Resource
    private RoleDao roleDao;

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
        Role role = roleId == null ? null : roleDao.findByIdAndIsDelFalse(roleId).orElse(null);
        if (role == null || StringUtils.isBlank(role.getNodeRights()) || !StringUtils.isNumeric(role.getNodeRights())) {
            return Collections.emptyList();
        }
        BigInteger nodeRights = new BigInteger(role.getNodeRights());
        return listActiveAuthNodes().stream()
                .filter(node -> RightsUtils.testRights(nodeRights, node.getId()))
                .toList();
    }

    @Override
    public List<AuthNode> listAuthNodeTreeByRole(Integer roleId) {
        List<AuthNode> nodes = listAuthNodesByRole(roleId);
        return buildTree(nodes);
    }

    @Override
    public List<String> listRolesForNode(AuthNode authNode) {
        if (authNode == null || authNode.getId() == null) {
            return Collections.emptyList();
        }
        return roleDao.findAllByIsDelFalseOrderByIdAsc().stream()
                .filter(role -> StringUtils.isNotBlank(role.getRoleName())
                        && StringUtils.isNotBlank(role.getNodeRights())
                        && StringUtils.isNumeric(role.getNodeRights())
                        && RightsUtils.testRights(new BigInteger(role.getNodeRights()), authNode.getId()))
                .map(Role::getRoleName)
                .distinct()
                .toList();
    }

    @Override
    public AuthNode addAuthNode(AuthNode authNode) {
        Validate.notNull(authNode, "权限节点不能为空");
        normalize(authNode);
        authNode.setId(null);
        authNode.setIsDel(false);
        authNode.setCreateTime(new java.util.Date());
        if (existsSameEndpoint(authNode.getPath(), authNode.getMethod(), null)) {
            throw new BaseException("权限节点已存在");
        }
        AuthNode saved = authNodeDao.save(authNode);
        refreshActiveAuthNodes();
        return saved;
    }

    @Override
    public AuthNode editAuthNode(AuthNode authNode) {
        Validate.notNull(authNode, "权限节点不能为空");
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
        existing.setParentId(authNode.getParentId());
        existing.setListOrder(authNode.getListOrder());
        existing.setUpdateTime(new java.util.Date());
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
        existing.setUpdateTime(new java.util.Date());
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
        nodes.forEach(this::normalize);
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
