package com.example.oauth.service;

import com.example.oauth.entity.SysMenu;
import com.example.oauth.vo.MenuVO;

import java.util.List;

/**
 * 菜单业务服务接口
 */
public interface MenuService {

    /**
     * 根据用户 ID 查询菜单列表
     */
    List<SysMenu> getMenusByUserId(Long userId);

    /**
     * 将扁平菜单列表构建为树形结构
     */
    List<MenuVO> buildMenuTree(List<SysMenu> menus);
}
