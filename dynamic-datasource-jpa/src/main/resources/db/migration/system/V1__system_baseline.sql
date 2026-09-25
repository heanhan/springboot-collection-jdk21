-- =============================================================================
-- 系统库基线 schema（Flyway V1）
-- 说明：
--   1. 仅管理系统库 sys_* 表；各租户库 schema 由租户侧独立迁移管理，不在此脚本内。
--   2. 对已存在数据的库，配合 baseline-on-migrate=true 会被基线化并跳过本脚本。
--   3. 兼容 MySQL 8.0+。
-- =============================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 权限节点表
-- ----------------------------
CREATE TABLE `sys_auth_node` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限节点id',
  `parent_id` int NOT NULL DEFAULT 0 COMMENT '父节点id',
  `name` varchar(100) NOT NULL COMMENT '节点名称',
  `path` varchar(255) NOT NULL COMMENT '请求路径',
  `method` varchar(10) NULL DEFAULT NULL COMMENT 'HTTP方法，为空匹配所有方法',
  `list_order` int NOT NULL DEFAULT 1 COMMENT '排序',
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限节点表';

INSERT INTO `sys_auth_node` (`id`, `parent_id`, `name`, `path`, `method`, `list_order`, `is_del`, `create_time`) VALUES
  (1, 0, '查询租户测试数据', '/api/test/**', NULL, 1, 0, NOW()),
  (2, 0, '新增租户账号', '/api/addTenantInfo', 'POST', 2, 0, NOW()),
  (3, 0, '管理租户数据源', '/api/dataSource/**', NULL, 3, 0, NOW());

-- ----------------------------
-- 菜单表
-- ----------------------------
CREATE TABLE `sys_menu` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单id',
  `name` varchar(255) NOT NULL COMMENT '菜单名称',
  `title` varchar(64) NULL DEFAULT NULL COMMENT '菜单标题',
  `menu_url` varchar(255) NULL DEFAULT NULL COMMENT '菜单地址',
  `parent_id` int NOT NULL COMMENT '父id',
  `list_order` int NOT NULL COMMENT '菜单排序',
  `icon` varchar(60) NULL DEFAULT NULL COMMENT '菜单图标',
  `is_show` tinyint(1) NULL DEFAULT 1,
  `type` tinyint(1) NULL DEFAULT 1 COMMENT '菜单类型 1：pc  2：小程序',
  `is_del` tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜单状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表';

-- ----------------------------
-- 角色表
-- ----------------------------
CREATE TABLE `sys_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称',
  `description` varchar(100) NULL DEFAULT NULL COMMENT '角色描述',
  `menu_rights` varchar(255) NULL DEFAULT NULL COMMENT '菜单权限',
  `node_rights` varchar(255) NULL DEFAULT NULL COMMENT '节点权限',
  `parent_id` int NOT NULL COMMENT '父id',
  `type` tinyint(2) NULL DEFAULT NULL COMMENT '角色类型',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

INSERT INTO `sys_role` (`id`, `role_name`, `description`, `menu_rights`, `node_rights`, `parent_id`, `type`, `create_time`, `is_del`) VALUES
  (1, 'ROLE_TENANT_USER', '普通租户用户', NULL, '2', 0, NULL, NOW(), 0),
  (2, 'ROLE_TENANT_ADMIN', '租户管理员', NULL, '14', 0, NULL, NOW(), 0);

-- ----------------------------
-- 租户表
-- ----------------------------
CREATE TABLE `sys_tenant` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_name` varchar(100) NULL DEFAULT NULL,
  `email` varchar(64) NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) NULL DEFAULT NULL COMMENT '联系电话',
  `logo_url` varchar(255) NULL DEFAULT NULL COMMENT 'logo地址',
  `is_del` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户表';

INSERT INTO `sys_tenant` (`id`, `tenant_name`, `is_del`) VALUES
  (0, '系统租户', 0);

-- ----------------------------
-- 租户数据源连接信息表
-- ----------------------------
CREATE TABLE `sys_tenant_data` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` int NOT NULL COMMENT '租户id',
  `url` varchar(255) NULL DEFAULT NULL,
  `username` varchar(50) NULL DEFAULT NULL,
  `password` varchar(255) NULL DEFAULT NULL COMMENT '连接密码；开启加密后为 Druid ConfigFilter 密文',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `is_del` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户数据源连接信息表';

-- ----------------------------
-- 系统用户表
-- ----------------------------
CREATE TABLE `sys_user` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `tenant_id` int NULL DEFAULT NULL,
  `role_id` int NULL DEFAULT NULL COMMENT '角色id',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码(BCrypt)',
  `nickname` varchar(32) NULL DEFAULT NULL COMMENT '昵称',
  `type` tinyint(1) NULL DEFAULT NULL,
  `email` varchar(32) NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(16) NULL DEFAULT NULL COMMENT '电话号码',
  `avatar` varchar(200) NULL DEFAULT NULL COMMENT '头像',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_phone` (`phone`),
  INDEX `idx_username` (`username`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户表';

-- 初始超级管理员，密码 123456（BCrypt），部署后必须立即修改
INSERT INTO `sys_user` (`id`, `tenant_id`, `role_id`, `username`, `password`, `status`, `create_time`) VALUES
  (1, 0, 2, 'admin', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', 1, NOW());
