package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author zhaojh
 */
@Data
@Entity
@Table(name ="sys_menu")
@DynamicInsert
@DynamicUpdate
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull(message = "id不能为空", groups = {Get.class, Delete.class, Edit.class})
    private Integer id;

    /**
     * 菜单名称
     */

    @NotBlank(message = "name不能为空", groups = {Add.class})
    @Column(name = "name")
    private String name;

    /**
     * 标题
     */
    @NotBlank(message = "title不能为空", groups = {Add.class})
    @Column(name = "title")
    private String title;

    /**
     * 菜单地址
     */
    @NotBlank(message = "menuUrl不能为空", groups = {Add.class})
    @Column(name = "menu_url")
    private String menuUrl;

    /**
     * 父id
     */
    @Column(name = "parent_id")
    private Integer parentId;

    /**
     * 菜单排序
     */
    @Column(name = "list_order")
    private Integer listOrder;

    /**
     * 菜单图标
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 菜单类型
     */
    @Column(name = "type")
    private Integer type;

    /**
     * 是否显示
     */
    @Column(name = "is_show")
    private Integer isShow;

    /**
     * 菜单状态
     */
    @Column(name = "is_del")
    private Boolean isDel;

    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "create_time")
    private Date createTime;


    /**
     * 子menu
     */
    @Transient
    private List<Menu> children;

    @Transient
    private boolean checked;

    public interface Add {

    }

    public interface Edit {

    }

    public interface Delete {

    }

    public interface Get {

    }

}
