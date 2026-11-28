package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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

@Data
@Entity
@Table(name = "sys_tenant")
@DynamicInsert
@DynamicUpdate
public class Tenant implements Serializable {

    private static final long serialVersionUID = 5757214508891529086L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_code")
    private String tenantCode;

    @NotBlank(message = "tenantName不能为空")
    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "status")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "provision_status")
    private String provisionStatus;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Column(name = "provision_error")
    private String provisionError;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "destroyed_at")
    private LocalDateTime destroyedAt;

    @Column(name = "is_del")
    private Boolean isDel = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
