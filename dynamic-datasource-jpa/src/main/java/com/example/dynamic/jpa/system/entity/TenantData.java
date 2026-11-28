package com.example.dynamic.jpa.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_tenant_data")
@DynamicInsert
@DynamicUpdate
public class TenantData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Integer tenantId;

    @JsonIgnore
    @Column(name = "url")
    private String url;

    @JsonIgnore
    @Column(name = "username")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Column(name = "password_cipher_version")
    private Integer passwordCipherVersion;

    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    @Column(name = "is_del")
    private Boolean isDel = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
