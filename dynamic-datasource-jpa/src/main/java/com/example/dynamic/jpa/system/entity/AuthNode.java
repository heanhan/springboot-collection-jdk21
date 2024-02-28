package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 节点表
 * </p>
 *
 * @author zhaojh
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "sys_auth_node")
@DynamicInsert
@DynamicUpdate
public class AuthNode implements Serializable {

    private static final long serialVersionUID = -1622126583445395333L;

    @NotNull(message = "id不能为空", groups = {Delete.class, Get.class, Edit.class})
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 父节点id
     */
    @Column(name = "parent_id")
    private Integer parentId;

    /**
     * 节点名称
     */
    @NotBlank(message = "name不能为空", groups = {Add.class})
    @Column(name = "name")
    private String name;

    /**
     * 路径
     */
    @NotBlank(message = "path不能为空", groups = {Add.class})
    @Column(name = "path")
    private String path;

    /**
     * 排序
     */
    @Column(name = "list_order")
    private Integer listOrder;

    /**
     * 状态 1=正常 2=停用 -1=软删除
     */
    @Column(name = "is_del")
    private Boolean isDel;

    /**
     * 子menu
     */
    @Transient
    private List<AuthNode> children;

    /**
     * 是否选择
     */
    @Transient
    private boolean checked;

    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    private Date updateTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH-mm-ss")
    private Date createTime;

    public interface Add {

    }

    public interface Edit {

    }

    public interface Delete {

    }

    public interface Get {

    }
}
