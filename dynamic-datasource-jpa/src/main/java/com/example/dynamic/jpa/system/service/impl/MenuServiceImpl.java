package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.MenuDao;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.service.MenuService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author zhaojh
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MenuServiceImpl implements MenuService {

    @Resource
    public MenuDao menuDao;


    @Override
    public List<Menu> listByPid(Integer parentId) {
//        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
//        wrapper.lambda().eq(Menu::getIsDel, false);
//        if (parentId == null) {
//            parentId = SystemConstant.ROOT_PARENT_ID;
//        }
//        wrapper.lambda().eq(Menu::getParentId, parentId);
//        wrapper.lambda().orderByAsc(Menu::getListOrder);
        return null;
    }

    @Override
    public List<Menu> listByPidInTree(Integer parentId) {
        List<Menu> menus = listByPid(parentId);
        menus.forEach(menu -> menu.setChildren(listByPidInTree(menu.getId())));
        return menus;
    }

    @Override
    public List<Menu> listAll() {
//        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
//        wrapper.lambda().eq(Menu::getIsDel, false);
//        wrapper.lambda().orderByAsc(Menu::getListOrder);
        List<Menu> all = menuDao.findAll();
        return all;
    }

    @Override
    public Menu addMenu(Menu menu) {
        if (menu.getListOrder() == null) {
            menu.setListOrder(1);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        Menu save = menuDao.save(menu);
        return save;
    }

    @Override
    public Menu getMenuById(Integer id) {
//        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
//        wrapper.lambda().eq(Menu::getIsDel, false);
//        wrapper.lambda().eq(Menu::getId, id);
        Menu menu = menuDao.findById(id).get();
        return menu;
    }

    @Override
    public Menu editMenu(Menu menu) {
        Validate.notNull(menu.getId(), "menu id 不允许为空");
        Menu menuById = getMenuById(menu.getId());
        if (menuById == null) {
            throw new BaseException("菜单不存在");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        return menuDao.save(menu);
    }


    @Override
    public boolean deleteMenu(Integer id) {
        Validate.notNull(id, "menu id 不允许为空");
        Menu menu = getMenuById(id);
        if (menu == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "菜单不存在");
        }
        menu.setIsDel(true);
        //todo 后续需要实现
        return true;
    }

}
