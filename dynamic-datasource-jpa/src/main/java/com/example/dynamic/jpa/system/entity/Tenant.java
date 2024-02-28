package com.example.dynamic.jpa.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * saas租户
 * </p>
 *
 * @author zhaojh
 * @since 2022-11-05
 */
@Data
@Entity
@Table(name ="sys_tenant")
@DynamicInsert
@DynamicUpdate
public class Tenant implements Serializable {

    private static final long serialVersionUID = 5757214508891529086L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @NotBlank(message = "tenantName不能为空")
    @Column(name = "tenant_name")
    private String tenantName;

    /**
     * 邮箱
     */
    @Column(name = "email")
    private String email;

    /**
     * 电话号码
     */
    @NotBlank(message = "phone不能为空")
    @Column(name = "phone")
    private String phone;

    /**
     * logo地址
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * 状态
     */
    @Column(name = "is_del")
    private Boolean isDel;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
