-- ============================================================
-- OAuth2.0 授权中心 - 数据库建表脚本
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `oauth_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `oauth_auth`;

-- ==================== 系统用户表 ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL COMMENT '用户ID（Snowflake）',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
    `gender`      INT          DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `status`      INT          DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ==================== 系统角色表 ====================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL COMMENT '角色ID（Snowflake）',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',
    `status`      INT          DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ==================== 系统权限表 ====================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id`          BIGINT       NOT NULL COMMENT '权限ID（Snowflake）',
    `perm_name`   VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `perm_code`   VARCHAR(128) NOT NULL COMMENT '权限编码',
    `type`        INT          DEFAULT 3 COMMENT '权限类型：1-菜单 2-按钮 3-接口',
    `url`         VARCHAR(256) DEFAULT NULL COMMENT '关联URL',
    `method`      VARCHAR(64)  DEFAULT NULL COMMENT 'HTTP方法',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',
    `status`      INT          DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- ==================== 系统菜单表 ====================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`         BIGINT       NOT NULL COMMENT '菜单ID（Snowflake）',
    `parent_id`  BIGINT       DEFAULT 0 COMMENT '父菜单ID（0表示根）',
    `menu_name`  VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `path`       VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    `component`  VARCHAR(128) DEFAULT NULL COMMENT '前端组件路径',
    `icon`       VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    `type`       INT          DEFAULT 0 COMMENT '类型：0-目录 1-菜单 2-按钮',
    `permission` VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    `sort`       INT          DEFAULT 0 COMMENT '排序号',
    `visible`    INT          DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
    `status`     INT          DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- ==================== 用户-角色关联表 ====================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- ==================== 角色-权限关联表 ====================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- ==================== 角色-菜单关联表 ====================
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- ==================== OAuth 客户端表（JDBC 存储，多客户端 client_credentials） ====================
DROP TABLE IF EXISTS `oauth_client`;
CREATE TABLE `oauth_client` (
    `id`                    BIGINT       NOT NULL COMMENT '主键ID（Snowflake）',
    `client_id`             VARCHAR(64)  NOT NULL COMMENT '客户端标识',
    `client_secret`         VARCHAR(128) NOT NULL COMMENT '客户端密钥（BCrypt）',
    `client_name`           VARCHAR(128) DEFAULT NULL COMMENT '客户端名称',
    `scopes`                VARCHAR(256) DEFAULT NULL COMMENT '授权范围（逗号分隔）',
    `grant_types`           VARCHAR(128) DEFAULT 'client_credentials' COMMENT '授权模式（逗号分隔）',
    `access_token_validity` BIGINT       DEFAULT NULL COMMENT 'Access Token 有效期（秒），空则用全局默认',
    `status`                INT          DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time`           DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth 客户端表';
