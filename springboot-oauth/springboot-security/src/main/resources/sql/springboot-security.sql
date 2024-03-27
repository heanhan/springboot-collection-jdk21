/*
 Navicat Premium Data Transfer

 Source Server         : localhost-macbookpro
 Source Server Type    : MySQL
 Source Server Version : 80029 (8.0.29)
 Source Host           : localhost:3306
 Source Schema         : springboot-security

 Target Server Type    : MySQL
 Target Server Version : 80029 (8.0.29)
 File Encoding         : 65001

 Date: 27/03/2024 14:59:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_authority`;
CREATE TABLE `sys_authority` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `authority` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `authority` (`authority`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

-- ----------------------------
-- Records of sys_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_authority` (`id`, `authority`) VALUES (1, '权限1');
INSERT INTO `sys_authority` (`id`, `authority`) VALUES (2, '权限2');
INSERT INTO `sys_authority` (`id`, `authority`) VALUES (3, '权限3');
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` (`id`, `role`) VALUES (2, '用户');
INSERT INTO `sys_role` (`id`, `role`) VALUES (1, '管理员');
COMMIT;

-- ----------------------------
-- Table structure for sys_role_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_authority`;
CREATE TABLE `sys_role_authority` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role` varchar(8) NOT NULL COMMENT '角色;不唯一',
  `authority` varchar(16) NOT NULL COMMENT '权限;不唯一',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限表';

-- ----------------------------
-- Records of sys_role_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_authority` (`id`, `role`, `authority`) VALUES (1, '管理员', '权限1');
INSERT INTO `sys_role_authority` (`id`, `role`, `authority`) VALUES (2, '管理员', '权限2');
INSERT INTO `sys_role_authority` (`id`, `role`, `authority`) VALUES (3, '管理员', '权限3');
INSERT INTO `sys_role_authority` (`id`, `role`, `authority`) VALUES (4, '用户', '权限1');
INSERT INTO `sys_role_authority` (`id`, `role`, `authority`) VALUES (5, '用户', '权限2');
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`) VALUES (1, '艾伦', '$2a$10$3OF9ij55dB7X2ffXby16Qu8n6Y96NV.RtHcza4vWO1EjoFO2JrsiW');
COMMIT;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(16) NOT NULL COMMENT '用户名;不唯一',
  `role` varchar(8) NOT NULL COMMENT '角色;不唯一',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_role` (`id`, `username`, `role`) VALUES (1, '艾伦', '管理员');
INSERT INTO `sys_user_role` (`id`, `username`, `role`) VALUES (2, '艾伦', '用户');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
