package com.example.dynamic.jpa.system.service;


import com.example.dynamic.jpa.system.entity.Menu;

import java.util.List;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author zhaojh
 */
public interface MenuService {
    /**
     * 根据父菜单id获取下面的所有菜单
     *
     * @param parentId 父菜单id
     * @return java.util.List<cn.greenbon.api.business.system.bean.Menu>
     */
    List<Menu> listByPid(Integer parentId);

    /**
     * 根据父菜单id获取下面的所有菜单,以树形结构展示
     *
     * @param parentId 父菜单id
     * @return java.util.List<cn.greenbon.api.business.system.bean.Menu>
     */
    List<Menu> listByPidInTree(Integer parentId);

    /**
     * 获取所有菜单
     *
     * @return java.util.List<cn.greenbon.api.business.system.bean.Menu>
     */
    List<Menu> listAll();

    /**
     * 添加菜单
     *
     * @param menu 菜单对象
     * @return boolean
     */
    Menu addMenu(Menu menu);

    /**
     * 根据id获取菜单
     *
     * @param id id
     * @return cn.greenbon.api.business.system.bean.Menu
     */
    Menu getMenuById(Integer id);

    /**
     * 编辑菜单
     *
     * @param menu 菜单对象
     * @return boolean
     */
    Menu editMenu(Menu menu);

    /**
     * 删除菜单
     *
     * @param id 菜单id
     * @return boolean
     */
    boolean deleteMenu(Integer id);
}
