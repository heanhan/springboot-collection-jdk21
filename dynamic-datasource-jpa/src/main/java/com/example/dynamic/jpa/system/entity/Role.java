package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author zhaojh
 */
@Data
@Entity
@Table(name ="sys_role")
@DynamicInsert
@DynamicUpdate
public class Role implements Serializable {

    private static final long serialVersionUID = -7605676229080298680L;

    /**
     * 角色id
     */
    @Id
    @NotNull(message = "role的Id不能为空", groups = {User.Delete.class, User.Edit.class, User.Get.class})
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 角色名称
     */
    @NotBlank(message = "roleName不能为空", groups = {Add.class})
    @Column(name = "role_name")
    private String roleName;

    /**
     * 权限描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 菜单权限
     */
    @Column(name = "menu_rights")
    private String menuRights;

    /**
     * 节点权限
     */
    @Column(name = "node_rights")
    private String nodeRights;

    /**
     * 父id
     */
    @Column(name = "parent_id")
    private Integer parentId;

    /**
     * 角色类型
     */
    @Column(name = "type")
    private Integer type;

    /**
     * 状态
     */
    @Column(name = "is_del")
    private Boolean isDel;

    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    @Column(name = "create_time")
    private LocalDateTime createTime;

    public interface Add {

    }
}
