package com.example.dynamic.jpa.system.service.impl;

import com.example.dynamic.jpa.common.constants.SystemConstant;
import com.example.dynamic.jpa.exception.BaseException;
import com.example.dynamic.jpa.exception.ExceptionCode;
import com.example.dynamic.jpa.system.dao.MenuDao;
import com.example.dynamic.jpa.system.entity.Menu;
import com.example.dynamic.jpa.system.service.MenuService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
        int pid = parentId == null ? SystemConstant.ROOT_PARENT_ID : parentId;
        return menuDao.findAllByParentIdAndIsDelFalseOrderByListOrderAscIdAsc(pid);
    }

    @Override
    public List<Menu> listByPidInTree(Integer parentId) {
        List<Menu> menus = listByPid(parentId);
        menus.forEach(menu -> menu.setChildren(listByPidInTree(menu.getId())));
        return menus;
    }

    @Override
    public List<Menu> listAll() {
        return menuDao.findAllByIsDelFalseOrderByListOrderAscIdAsc();
    }

    @Override
    public Menu addMenu(Menu menu) {
        Validate.notNull(menu, "菜单不能为空");
        if (StringUtils.isBlank(menu.getName()) || StringUtils.isBlank(menu.getTitle())
                || StringUtils.isBlank(menu.getMenuUrl())) {
            throw new BaseException("菜单名称、标题和地址不能为空");
        }
        menu.setId(null);
        if (menu.getListOrder() == null) {
            menu.setListOrder(1);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(SystemConstant.ROOT_PARENT_ID);
        }
        menu.setIsDel(false);
        menu.setCreateTime(new Date());
        return menuDao.save(menu);
    }

    @Override
    public Menu getMenuById(Integer id) {
        return id == null ? null : menuDao.findByIdAndIsDelFalse(id).orElse(null);
    }

    @Override
    public Menu editMenu(Menu menu) {
        Validate.notNull(menu.getId(), "menu id 不允许为空");
        Menu existing = getMenuById(menu.getId());
        if (existing == null) {
            throw new BaseException("菜单不存在");
        }
        if (StringUtils.isNotBlank(menu.getName())) {
            existing.setName(menu.getName().trim());
        }
        if (StringUtils.isNotBlank(menu.getTitle())) {
            existing.setTitle(menu.getTitle().trim());
        }
        if (StringUtils.isNotBlank(menu.getMenuUrl())) {
            existing.setMenuUrl(menu.getMenuUrl().trim());
        }
        if (menu.getParentId() != null) {
            existing.setParentId(menu.getParentId());
        }
        if (menu.getListOrder() != null) {
            existing.setListOrder(menu.getListOrder());
        }
        existing.setIcon(menu.getIcon());
        existing.setType(menu.getType());
        existing.setIsShow(menu.getIsShow());
        existing.setUpdateTime(new Date());
        return menuDao.save(existing);
    }


    @Override
    public boolean deleteMenu(Integer id) {
        Validate.notNull(id, "menu id 不允许为空");
        Menu menu = getMenuById(id);
        if (menu == null) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "菜单不存在");
        }
        if (menuDao.existsByParentIdAndIsDelFalse(id)) {
            throw new BaseException(ExceptionCode.DELETE.getCode(), "存在子菜单，不能删除");
        }
        menu.setIsDel(true);
        menu.setUpdateTime(new Date());
        menuDao.save(menu);
        return true;
    }

}
