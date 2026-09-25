package com.example.dynamic.jpa.system.service;

import com.example.dynamic.jpa.system.entity.AuthNode;

import java.util.List;

/**
 * 权限节点服务
 *
 * @author zhaojh
 */
public interface AuthNodeService {

    /**
     * 获取全部有效权限节点
     */
    List<AuthNode> listActiveAuthNodes();

    /**
     * 重新加载权限节点缓存
     */
    List<AuthNode> refreshActiveAuthNodes();

    /**
     * 获取角色拥有的有效权限节点
     */
    List<AuthNode> listAuthNodesByRole(Integer roleId);

    /**
     * 获取角色权限节点树
     */
    List<AuthNode> listAuthNodeTreeByRole(Integer roleId);

    /**
     * 获取拥有指定权限节点的角色名称
     */
    List<String> listRolesForNode(AuthNode authNode);

    AuthNode addAuthNode(AuthNode authNode);

    AuthNode editAuthNode(AuthNode authNode);

    boolean deleteAuthNode(Integer id);

    AuthNode getAuthNodeById(Integer id);
}
