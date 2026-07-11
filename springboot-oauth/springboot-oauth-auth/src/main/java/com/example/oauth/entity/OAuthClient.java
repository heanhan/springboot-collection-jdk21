package com.example.oauth.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * OAuth 客户端实体（JDBC 存储，替代原授权服务器的内存客户端配置）
 * <p>
 * 支持多客户端接入：每个第三方应用/微服务以 client_id + client_secret 通过
 * client_credentials 模式获取机器令牌（M2M Token），令牌同样使用 RS256 签名，
 * 下游资源服务器可通过 JWK 端点离线验签。
 */
@Data
@Entity
@Table(name = "oauth_client")
public class OAuthClient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "snowflakeId")
    @GenericGenerator(name = "snowflakeId", strategy = "com.example.oauth.config.SnowflakeIdGenerator")
    @Column(name = "id")
    private Long id;

    /** 客户端标识 */
    @Column(name = "client_id", length = 64, unique = true, nullable = false)
    private String clientId;

    /** 客户端密钥（BCrypt 加密存储） */
    @Column(name = "client_secret", length = 128, nullable = false)
    private String clientSecret;

    /** 客户端名称 */
    @Column(name = "client_name", length = 128)
    private String clientName;

    /** 授权范围（逗号分隔，如 read,write） */
    @Column(name = "scopes", length = 256)
    private String scopes;

    /** 授权模式（逗号分隔，当前支持 client_credentials） */
    @Column(name = "grant_types", length = 128)
    private String grantTypes;

    /** Access Token 有效期（秒），为空则使用全局默认值 */
    @Column(name = "access_token_validity")
    private Long accessTokenValidity;

    /** 状态：0-禁用 1-启用 */
    @Column(name = "status")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
