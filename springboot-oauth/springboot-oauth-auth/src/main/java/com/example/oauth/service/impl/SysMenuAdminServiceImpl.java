package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.entity.SysMenu;
import com.example.oauth.repository.SysMenuRepository;
import com.example.oauth.repository.SysRoleMenuRepository;
import com.example.oauth.service.SysMenuAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理服务实现
 */
@Slf4j
@Service
public class SysMenuAdminServiceImpl implements SysMenuAdminService {

    private final SysMenuRepository menuRepository;
    private final SysRoleMenuRepository roleMenuRepository;

    public SysMenuAdminServiceImpl(SysMenuRepository menuRepository,
                                   SysRoleMenuRepository roleMenuRepository) {
        this.menuRepository = menuRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    @Override
    public List<SysMenu> listAll() {
        return menuRepository.findAll(Sort.by(Sort.Direction.ASC, "sort"));
    }

    @Override
    public List<SysMenu> tree() {
        List<SysMenu> all = listAll();
        // 按 parentId 分组（null 视为根 0）
        Map<Long, List<SysMenu>> parentMap = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        // 挂载子节点
        all.forEach(m -> m.setChildren(parentMap.getOrDefault(m.getId(), new ArrayList<>())));
        // 返回根节点（parentId = 0）
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }

    @Override
    public SysMenu getById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "菜单不存在: " + id));
    }

    @Override
    @Transactional
    public SysMenu create(SysMenu menu) {
        if (!StringUtils.hasText(menu.getMenuName())) {
            throw new BizException(400, "菜单名称不能为空");
        }
        menu.setId(null);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        return menuRepository.save(menu);
    }

    @Override
    @Transactional
    public SysMenu update(SysMenu menu) {
        if (menu.getId() == null) {
            throw new BizException(400, "更新菜单时 ID 不能为空");
        }
        SysMenu existing = getById(menu.getId());
        existing.setParentId(menu.getParentId() == null ? 0L : menu.getParentId());
        existing.setMenuName(menu.getMenuName());
        existing.setPath(menu.getPath());
        existing.setComponent(menu.getComponent());
        existing.setIcon(menu.getIcon());
        existing.setType(menu.getType());
        existing.setPermission(menu.getPermission());
        if (menu.getSort() != null) {
            existing.setSort(menu.getSort());
        }
        if (menu.getVisible() != null) {
            existing.setVisible(menu.getVisible());
        }
        if (menu.getStatus() != null) {
            existing.setStatus(menu.getStatus());
        }
        return menuRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        // 存在子菜单时禁止删除
        List<SysMenu> children = menuRepository.findByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw new BizException(400, "存在子菜单，无法删除，请先删除子菜单");
        }
        roleMenuRepository.deleteByMenuId(id);
        menuRepository.deleteById(id);
    }
}
