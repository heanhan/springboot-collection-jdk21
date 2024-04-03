

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS `oauth_client_details`;
CREATE TABLE `oauth_client_details`  (
                                         `client_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                         `resource_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `client_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `scope` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `authorized_grant_types` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `web_server_redirect_uri` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `authorities` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `access_token_validity` int(0) NULL DEFAULT NULL,
                                         `refresh_token_validity` int(0) NULL DEFAULT NULL,
                                         `additional_information` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         `autoapprove` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                                         PRIMARY KEY (`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '终端信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of oauth_client_details
-- ----------------------------
INSERT INTO `oauth_client_details` VALUES ('yeee-springboot-vue', NULL, '96e79218965eb72c92a549dd5a330112', 'all', 'password,refresh_token,authorization_code,client_credentials', '', NULL, 864000, 6048000, NULL, 'true');
-- ----------------------------
-- Table structure for oauth_access_token
-- ----------------------------
DROP TABLE IF EXISTS `oauth_access_token`;
CREATE TABLE `oauth_access_token`  (
                                       `token_id` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `token` blob NULL,
                                       `authentication_id` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                                       `user_name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `client_id` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       `authentication` blob NULL,
                                       `refresh_token` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                       PRIMARY KEY (`authentication_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of oauth_access_token
-- ----------------------------
-- ----------------------------
-- Table structure for oauth_refresh_token
-- ----------------------------
DROP TABLE IF EXISTS `oauth_refresh_token`;
CREATE TABLE `oauth_refresh_token`  (
                                        `token_id` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                                        `token` blob NULL,
                                        `authentication` blob NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of oauth_refresh_token
-- ----------------------------
-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
                             `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '部门id',
                             `name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '部门名称',
                             `code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门代码',
                             `pid` int(0) NULL DEFAULT -1 COMMENT '父部门id',
                             `ancestors` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '祖级列表',
                             `status` int(0) NULL DEFAULT 0 COMMENT '部门状态（0正常 1停用）',
                             `sort` int(0) NULL DEFAULT 0 COMMENT '显示顺序',
                             `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
                             `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `idx_code`(`code`) USING BTREE,
                             INDEX `idx_parent_id`(`pid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 433 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO sys_dept
(id, name, code, pid, ancestors, status, sort, remark, create_by, create_time, update_by, update_time)
VALUES(433, '部门1', 'bumeng1', -1, NULL, 0, 0, '', 'admin', '2024-01-04 11:40:16', 'admin', '2024-01-04 11:40:16');
INSERT INTO sys_dept
(id, name, code, pid, ancestors, status, sort, remark, create_by, create_time, update_by, update_time)
VALUES(434, '部门2', 'bumeng2', -1, NULL, 0, 0, '', 'admin', '2024-01-04 11:40:26', 'admin', '2024-01-04 11:40:26');
INSERT INTO sys_dept
(id, name, code, pid, ancestors, status, sort, remark, create_by, create_time, update_by, update_time)
VALUES(435, '部门3', 'bumeng3', -1, NULL, 0, 0, '', 'admin', '2024-01-04 11:40:37', 'admin', '2024-01-04 11:40:37');
-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
                             `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `pid` bigint(0) NULL DEFAULT NULL COMMENT '父ID',
                             `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
                             `url` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '资源路径',
                             `perm` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限标识',
                             `icon` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图标',
                             `seq` smallint(0) NULL DEFAULT NULL COMMENT '排序',
                             `remark` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
                             `type` tinyint(0) NULL DEFAULT NULL COMMENT '类型 (0：菜单， 1：功能， 2：伪菜单)',
                             `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                             `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
                             `update_time` datetime(0) NULL DEFAULT NULL COMMENT '修改时间',
                             `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4353221119792138 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统权限资源' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4059916341400576, NULL, '系统管理', '', NULL, 'icon-system', 100, '', 0, NULL, NULL, '2019-02-13 23:24:22', NULL);
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4059916791092224, 4059916341400576, '用户管理', 'sys/user/user', NULL, 'icon-admin', 5, 'view/sys/user/user.html', 0, NULL, NULL, '2019-02-11 15:16:48', NULL);
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4059917154079744, 4059916341400576, '角色管理', 'sys/role/role', NULL, 'icon-geren', 10, 'view/sys/role/role.html', 0, NULL, NULL, '2019-02-11 15:42:24', NULL);
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4059917584143360, 4059916341400576, '菜单管理', 'sys/menu/menu', NULL, 'icon-editor', 15, 'view/sys/menu/menu.html', 0, NULL, NULL, '2019-02-11 15:42:45', NULL);
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297179758302211, 4059919704003584, '修改', NULL, 'sys:param:upd', NULL, 30, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735991448577, 4059917154079744, '查看', NULL, 'sys:role:page, sys:role:list, sys:role:info,sys:role:listSet,sys:role:exist', NULL, 10, NULL, 1, NULL, 'auto', '2024-01-18 17:49:42', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735991448578, 4059917154079744, '新增', NULL, 'sys:role:add', NULL, 20, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735991448579, 4059917154079744, '修改', NULL, 'sys:role:upd', NULL, 30, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735991448580, 4059917154079744, '删除', NULL, 'sys:role:del', NULL, 40, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735992218625, 4059917584143360, '查看', NULL, 'sys:menu:page, sys:menu:list, sys:menu:info,sys:menu:listSet', NULL, 10, NULL, 1, NULL, 'auto', '2024-01-18 17:41:19', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735992218626, 4059917584143360, '新增', NULL, 'sys:menu:add', NULL, 20, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735992218627, 4059917584143360, '修改', NULL, 'sys:menu:upd', NULL, 30, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735992218628, 4059917584143360, '删除', NULL, 'sys:menu:del', NULL, 40, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735995577345, 4059916791092224, '查看', NULL, 'sys:user:page, sys:user:list, sys:user:info,sys:user:exist,sys:user:getUserInfo', NULL, 10, NULL, 1, NULL, 'auto', '2024-01-18 17:37:47', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735995577346, 4059916791092224, '新增', NULL, 'sys:user:add', NULL, 20, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735995577347, 4059916791092224, '修改', NULL, 'sys:user:upd', NULL, 30, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4297735995577348, 4059916791092224, '删除', NULL, 'sys:user:del', NULL, 40, NULL, 1, NULL, 'auto', NULL, 'auto');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4298067236262912, 4059916791092224, '修改密码', '', 'sys:user:updPwd', '', 80, '', 1, '2019-02-26 16:03:00', 'admin', '2019-02-26 16:03:00', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4347326007383040, 4059916341400576, '组织机构', 'sys/org/org', NULL, 'icon-cluster', 30, NULL, 0, NULL, 'auto', '2019-04-02 12:34:22', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4353221119792134, 4347326007383040, '详情', '', 'sys:dept:info,sys:dept:page,sys:dept:exist,sys:dept:listSet', '', 1, NULL, 1, '2022-05-29 14:52:26', 'admin', '2024-01-18 17:30:29', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4353221119792135, 4347326007383040, '删除', '', 'sys:dept:del', '', 1, NULL, 1, '2022-05-29 14:53:00', 'admin', '2022-05-29 14:53:00', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4353221119792136, 4347326007383040, '编辑', '', 'sys:dept:upd', '', 1, NULL, 1, '2022-05-29 14:53:18', 'admin', '2022-05-29 14:53:18', 'admin');
INSERT INTO sys_menu
(id, pid, name, url, perm, icon, seq, remark, `type`, create_time, create_by, update_time, update_by)
VALUES(4353221119792137, 4347326007383040, '新增', '', 'sys:dept:add', '', 1, NULL, 1, '2022-05-29 14:53:53', 'admin', '2022-05-29 14:53:53', 'admin');
-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
                             `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色id',
                             `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色code',
                             `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
                             `status` int(0) NULL DEFAULT 0 COMMENT '状态 （0正常 1停用）',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
                             `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
                             `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
                             `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ADMIN', '管理员', 0, '管理员', '1', '2021-11-24 11:33:10', 'admin', NULL);
INSERT INTO sys_role
(id, code, name, status, remark, create_by, create_time, update_by, update_time)
VALUES(2, 'COMMON_USER', 'COMMON_USER', 0, NULL, 'admin', '2023-12-22 15:28:13', 'admin', '2023-12-22 15:28:13');
-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
                                  `id` bigint(0) NOT NULL COMMENT 'ID',
                                  `role_id` bigint(0) NULL DEFAULT NULL COMMENT '被授权对象ID',
                                  `menu_id` bigint(0) NULL DEFAULT NULL COMMENT '授权资源表ID',
                                  `remark` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
                                  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                                  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
                                  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '修改时间',
                                  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统授权' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO sys_role_menu
(id, role_id, menu_id, remark, create_time, create_by, update_time, update_by)
VALUES(1742756061720842241, 2, 4353221119792141, NULL, '2024-01-04 11:53:19', 'admin', '2024-01-04 11:53:19', 'admin');
INSERT INTO sys_role_menu
(id, role_id, menu_id, remark, create_time, create_by, update_time, update_by)
VALUES(1742756061733425153, 2, 4353221119792138, NULL, '2024-01-04 11:53:19', 'admin', '2024-01-04 11:53:19', 'admin');
-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户名',
                             `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
                             `nick_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '花名',
                             `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
                             `phone` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号码',
                             `sex` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '性别',
                             `avatar` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
                             `dept_id` int(0) UNSIGNED NULL DEFAULT NULL COMMENT '部门名称',
                             `job_id` int(0) UNSIGNED NULL DEFAULT NULL COMMENT '岗位名称',
                             `state` int(0) NULL DEFAULT 0 COMMENT '状态：（0正常 1停用）',
                             `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '最后登陆时间',
                             `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建日期',
                             `update_time` datetime(0) NULL DEFAULT NULL COMMENT '修改时间',
                             `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
                             `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
                             `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
                             `super_admin` tinyint(0) NULL DEFAULT NULL COMMENT '超级管理员',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `username_idx`(`username`) USING BTREE,
                             INDEX `dept_idx`(`dept_id`) USING BTREE,
                             INDEX `job_idx`(`job_id`) USING BTREE,
                             INDEX `create_time_idx`(`create_time`) USING BTREE,
                             INDEX `update_time_idx`(`update_time`) USING BTREE,
                             INDEX `email_idx`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO sys_user
(id, username, password, nick_name, email, phone, sex, avatar, dept_id, job_id, state, last_login_time, create_time, update_time, create_by, update_by, remark, super_admin)
VALUES(1, 'admin', '9db06bcff9248837f86d1a6bcf41c9e7', '超级管理员', NULL, '13888888888', NULL, NULL, 8, NULL, 0, NULL, '2021-07-12 11:24:02', '2023-12-28 15:57:34', '1', 'admin', '管理员', 1);
INSERT INTO sys_user
(id, username, password, nick_name, email, phone, sex, avatar, dept_id, job_id, state, last_login_time, create_time, update_time, create_by, update_by, remark, super_admin)
VALUES(3, 'yonghu1', '9db06bcff9248837f86d1a6bcf41c9e7', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO sys_user
(id, username, password, nick_name, email, phone, sex, avatar, dept_id, job_id, state, last_login_time, create_time, update_time, create_by, update_by, remark, super_admin)
VALUES(4, 'yonghu2', '9db06bcff9248837f86d1a6bcf41c9e7', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO sys_user
(id, username, password, nick_name, email, phone, sex, avatar, dept_id, job_id, state, last_login_time, create_time, update_time, create_by, update_by, remark, super_admin)
VALUES(5, 'yonghu3', '9db06bcff9248837f86d1a6bcf41c9e7', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dept`;
CREATE TABLE `sys_user_dept`  (
                                  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '部门id',
                                  `user_id` int(0) NULL DEFAULT -1 COMMENT '用户ID',
                                  `dept_id` int(0) NULL DEFAULT NULL COMMENT '部门ID',
                                  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_parent_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;
INSERT INTO sys_user_dept
(id, user_id, dept_id, create_by, create_time, update_by, update_time)
VALUES(2, 1, 432, 'admin', '2023-12-28 15:57:34', 'admin', '2023-12-28 15:57:34');
INSERT INTO sys_user_dept
(id, user_id, dept_id, create_by, create_time, update_by, update_time)
VALUES(3, 1, 431, 'admin', '2023-12-28 15:57:34', 'admin', '2023-12-28 15:57:34');
INSERT INTO sys_user_dept
(id, user_id, dept_id, create_by, create_time, update_by, update_time)
VALUES(4, 3, 433, 'admin', '2024-01-04 11:51:27', 'admin', '2024-01-04 11:51:27');
INSERT INTO sys_user_dept
(id, user_id, dept_id, create_by, create_time, update_by, update_time)
VALUES(5, 4, 434, 'admin', '2024-01-04 11:51:37', 'admin', '2024-01-04 11:51:37');
INSERT INTO sys_user_dept
(id, user_id, dept_id, create_by, create_time, update_by, update_time)
VALUES(6, 5, 435, 'admin', '2024-01-04 11:51:47', 'admin', '2024-01-04 11:51:47');
-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
                                  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `user_id` int(0) UNSIGNED NOT NULL COMMENT '用户ID',
                                  `role_id` int(0) UNSIGNED NOT NULL COMMENT '角色ID',
                                  `create_by` varchar(32) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '创建人',
                                  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` varchar(32) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL COMMENT '更新人',
                                  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_user_id`(`user_id`) USING BTREE,
                                  INDEX `idx_role_id`(`role_id`) USING BTREE,
                                  INDEX `idx_user_role`(`role_id`, `user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 367 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;
INSERT INTO sys_user_role
(id, user_id, role_id, create_by, create_time, update_by, update_time)
VALUES(5, 1, 1, 'admin', '2023-12-28 15:57:34', 'admin', '2023-12-28 15:57:34');
INSERT INTO sys_user_role
(id, user_id, role_id, create_by, create_time, update_by, update_time)
VALUES(6, 1, 2, 'admin', '2023-12-28 15:57:34', 'admin', '2023-12-28 15:57:34');
INSERT INTO sys_user_role
(id, user_id, role_id, create_by, create_time, update_by, update_time)
VALUES(7, 3, 2, 'admin', '2024-01-04 11:51:27', 'admin', '2024-01-04 11:51:27');
INSERT INTO sys_user_role
(id, user_id, role_id, create_by, create_time, update_by, update_time)
VALUES(8, 4, 2, 'admin', '2024-01-04 11:51:37', 'admin', '2024-01-04 11:51:37');
INSERT INTO sys_user_role
(id, user_id, role_id, create_by, create_time, update_by, update_time)
VALUES(9, 5, 2, 'admin', '2024-01-04 11:51:47', 'admin', '2024-01-04 11:51:47');


SET FOREIGN_KEY_CHECKS = 1;
