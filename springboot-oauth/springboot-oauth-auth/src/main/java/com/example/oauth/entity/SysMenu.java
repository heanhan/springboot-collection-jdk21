package com.example.oauth.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统菜单实体
 * <p>
 * 支持树形结构（自引用 parent/children）。
 * 与角色的关系不再使用 @ManyToMany，改由关联实体 SysRoleMenu 显式映射。
 */
@Data
@Entity
@Table(name = "sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "snowflakeId")
    @GenericGenerator(name = "snowflakeId", strategy = "com.example.oauth.config.SnowflakeIdGenerator")
    @Column(name = "id")
    private Long id;

    /** 父菜单 ID（0 表示根菜单） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 菜单名称 */
    @Column(name = "menu_name", length = 64, nullable = false)
    private String menuName;

    /** 路由路径 */
    @Column(name = "path", length = 128)
    private String path;

    /** 前端组件路径 */
    @Column(name = "component", length = 128)
    private String component;

    /** 图标 */
    @Column(name = "icon", length = 64)
    private String icon;

    /** 类型：0-目录 1-菜单 2-按钮 */
    @Column(name = "type")
    private Integer type;

    /** 权限标识（如 user:list） */
    @Column(name = "permission", length = 128)
    private String permission;

    /** 排序号 */
    @Column(name = "sort")
    private Integer sort;

    /** 是否可见：0-隐藏 1-显示 */
    @Column(name = "visible")
    private Integer visible;

    /** 状态：0-禁用 1-启用 */
    @Column(name = "status")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /**
     * 子菜单（ transient，不持久化，用于构建菜单树）
     */
    @Transient
    private List<SysMenu> children = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
