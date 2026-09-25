
-- =============================================================================
-- 【仅本地/demo 引导脚本】非生产 schema 真相源！
--   1. 用途：本地/dev 一次性手工初始化系统库（含 CREATE DATABASE / DROP TABLE / demo 数据）。
--   2. 生产系统库 schema 由 Flyway 管理：见 db/migration/system/V1__system_baseline.sql（prod profile 自动执行）。
--   3. 两者字段若有出入，以 JPA 实体 + Flyway 脚本为准；本脚本含明文口令与 demo 租户，禁止用于生产。
--   4. 租户库（tenant_one/two/three）目前仍靠本目录 sql/tenant_*.sql 手工建，未纳入 Flyway。
-- 执行前请确认当前数据库与 application-dev.yml 中的 spring.datasource.url 一致
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `springboot-jpa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `springboot-jpa`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_auth_node
-- ----------------------------
DROP TABLE IF EXISTS `sys_auth_node`;
CREATE TABLE `sys_auth_node`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限节点id',
  `parent_id` int(11) NOT NULL DEFAULT 0 COMMENT '父节点id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点名称',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求路径',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'HTTP方法，为空匹配所有方法',
  `list_order` int(6) NOT NULL DEFAULT 1 COMMENT '排序',
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限节点表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_auth_node
-- ----------------------------
INSERT INTO `sys_auth_node` VALUES (1, 0, '查询租户测试数据', '/api/test/**', NULL, 1, 0, NOW(), NULL);
INSERT INTO `sys_auth_node` VALUES (2, 0, '新增租户账号', '/api/addTenantInfo', 'POST', 2, 0, NOW(), NULL);
INSERT INTO `sys_auth_node` VALUES (3, 0, '管理租户数据源', '/api/dataSource/**', NULL, 3, 0, NOW(), NULL);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单标题',
  `menu_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单地址',
  `parent_id` int(11) NOT NULL COMMENT '父id',
  `list_order` int(6) NOT NULL COMMENT '菜单排序',
  `icon` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `is_show` tinyint(1) NULL DEFAULT 1,
  `type` tinyint(1) NULL DEFAULT 1 COMMENT '菜单类型 1：pc  2：小程序',
  `is_del` tinyint(1) NOT NULL DEFAULT 1 COMMENT '菜单状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 438 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `menu_rights` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单权限',
  `node_rights` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '节点权限',
  `parent_id` int(11) NOT NULL COMMENT '父id',
  `type` tinyint(2) NULL DEFAULT NULL COMMENT '角色类型',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ROLE_TENANT_USER', '普通租户用户', NULL, '2', 0, NULL, '2021-05-28 15:54:19', NULL, 0);
INSERT INTO `sys_role` VALUES (2, 'ROLE_TENANT_ADMIN', '租户管理员', NULL, '14', 0, NULL, '2021-05-28 15:54:19', NULL, 0);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `is_del` tinyint(1) NULL DEFAULT NULL COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (0, '系统租户', NULL, NULL, NULL, 0);
INSERT INTO `sys_tenant` VALUES (1, '租户一', NULL, NOW(), NULL, 0);
INSERT INTO `sys_tenant` VALUES (2, '租户二', NULL, NOW(), NULL, 0);
INSERT INTO `sys_tenant` VALUES (3, '租户三', NULL, NOW(), NULL, 0);

-- ----------------------------
-- Table structure for sys_tenant_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_data`;
CREATE TABLE `sys_tenant_data`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` int(11) NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `is_del` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant_data
-- ----------------------------
INSERT INTO `sys_tenant_data` VALUES (1, 1, 'jdbc:mysql://172.16.75.105:3306/tenant_one?tinyInt1isBit=false&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowMultiQueries=true', 'root', 'admin123', NULL, NULL, 0);
INSERT INTO `sys_tenant_data` VALUES (2, 2, 'jdbc:mysql://172.16.75.105:3306/tenant_two?tinyInt1isBit=false&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowMultiQueries=true', 'root', 'admin123', NULL, NULL, 0);
INSERT INTO `sys_tenant_data` VALUES (3, 3, 'jdbc:mysql://172.16.75.105:3306/tenant_three?tinyInt1isBit=false&useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowMultiQueries=true', 'root', 'admin123', NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `tenant_id` int(11) NULL DEFAULT NULL,
  `role_id` int(11) NULL DEFAULT NULL COMMENT '角色id',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `type` tinyint(1) NULL DEFAULT NULL,
  `email` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '电话号码',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_phone`(`phone`) USING BTREE,
  INDEX `idx_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 270 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系統用戶表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- 初始化账号密码均为 123456，部署后应立即修改
-- ----------------------------
INSERT INTO `sys_user` VALUES (267, 1, 1, 'a', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:08', NULL);
INSERT INTO `sys_user` VALUES (268, 2, 1, 'b', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:08', NULL);
INSERT INTO `sys_user` VALUES (269, 3, 1, 'c', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:40', NULL);
INSERT INTO `sys_user` VALUES (270, 0, 2, 'admin', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:40', NULL);
INSERT INTO `sys_user` VALUES (271, 1, 2, 'tenant1_admin', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:40', NULL);
INSERT INTO `sys_user` VALUES (272, 2, 2, 'tenant2_admin', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:40', NULL);
INSERT INTO `sys_user` VALUES (273, 3, 2, 'tenant3_admin', '$2a$10$PG2beRhlJGGDbBYBNSLOB.T.T8WeoDYxCog67e2qDrRcSS99HbYx2', NULL, NULL, NULL, NULL, NULL, NULL, 1, '2021-05-28 15:51:40', NULL);

SET FOREIGN_KEY_CHECKS = 1;
