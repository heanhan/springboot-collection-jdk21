package com.example.dynamic.jpa.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: zhaojh
 * @ClassName: TenantDataInfo
 * @Description: 租户数据连接信息
 */
@Data
@Entity
@Table(name ="sys_tenant_data")
@DynamicInsert
@DynamicUpdate
public class TenantData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Integer tenantId;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @lombok.ToString.Exclude
    @Column(name = "password")
    private String password;

    /**
     * 状态
     */
    @Column(name = "is_del")
    private Boolean isDel = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
