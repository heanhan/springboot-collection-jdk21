package com.example.oauth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 VO
 */
@Data
public class MenuVO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String path;

    private String component;

    private String icon;

    private Integer type;

    private String permission;

    private Integer sort;

    private Integer visible;

    /** 子菜单 */
    private List<MenuVO> children = new ArrayList<>();
}
