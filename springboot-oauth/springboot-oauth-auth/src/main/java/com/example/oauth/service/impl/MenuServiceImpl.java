package com.example.oauth.service.impl;

import com.example.oauth.entity.SysMenu;
import com.example.oauth.repository.SysMenuRepository;
import com.example.oauth.service.MenuService;
import com.example.oauth.vo.MenuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单业务服务实现
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    private final SysMenuRepository sysMenuRepository;

    public MenuServiceImpl(SysMenuRepository sysMenuRepository) {
        this.sysMenuRepository = sysMenuRepository;
    }

    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        return sysMenuRepository.findMenusByUserId(userId);
    }

    @Override
    public List<MenuVO> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为 VO
        List<MenuVO> voList = menus.stream().map(this::convertToVO).collect(Collectors.toList());

        // 按 parentId 分组
        Map<Long, List<MenuVO>> parentMap = voList.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        // 设置子菜单
        voList.forEach(vo -> vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>())));

        // 返回根菜单（parentId = 0）
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }

    /**
     * SysMenu 转 MenuVO
     */
    private MenuVO convertToVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setType(menu.getType());
        vo.setPermission(menu.getPermission());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        return vo;
    }
}
