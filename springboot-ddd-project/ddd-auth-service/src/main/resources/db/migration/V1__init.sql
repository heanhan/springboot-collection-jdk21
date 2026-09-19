-- ==============================================================================
-- ddd-auth-service Flyway 初始化脚本 V1
-- ==============================================================================
-- 认证上下文只关心"凭据"，与 user-service 的业务档案分离。
-- user_id 与 user-service 中的 t_user.user_id 保持一致（由 auth 生成后通过 MQ 通知 user 建档）。
-- ==============================================================================

CREATE TABLE IF NOT EXISTS t_user_credential
(
    user_id         VARCHAR(32)  NOT NULL COMMENT '用户 ID (雪花，与 user-service 共享)',
    username        VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    password_hash   VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希 (含盐)',
    mobile          VARCHAR(20)  NOT NULL COMMENT '手机号，用于密码找回',
    email           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / LOCKED / DISABLED',
    fail_count      INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_until      DATETIME     DEFAULT NULL COMMENT '锁定截止时间',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    last_login_ip   VARCHAR(64)  DEFAULT NULL COMMENT '最近登录 IP',
    pwd_update_time DATETIME     DEFAULT NULL COMMENT '密码最近修改时间',
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username, deleted),
    UNIQUE KEY uk_mobile (mobile, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='认证凭据 (UserCredential 聚合根)';

-- ---------------------------------------------------------------------------
-- t_login_session 登录会话 (可选，用于强制下线审计)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_login_session
(
    session_id  VARCHAR(64) NOT NULL COMMENT '会话 ID (tokenId)',
    user_id     VARCHAR(32) NOT NULL,
    device      VARCHAR(64)  DEFAULT NULL COMMENT '登录设备',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '登录 IP',
    login_time  DATETIME    NOT NULL,
    expires_at  DATETIME    NOT NULL,
    revoked     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已注销',
    create_time DATETIME    NOT NULL,
    PRIMARY KEY (session_id),
    KEY idx_user_id (user_id, revoked)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='登录会话 (LoginSession 实体)';

-- ==============================================================================
-- 初始化：默认管理员账号
-- ==============================================================================
-- 密码: admin123 (BCrypt hash)
INSERT INTO t_user_credential
(user_id, username, password_hash, mobile, status, create_time, update_time, version, deleted)
VALUES ('1', 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
        '13800000000', 'ACTIVE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
