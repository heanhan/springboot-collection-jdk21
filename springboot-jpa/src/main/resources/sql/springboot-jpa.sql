/*
 Navicat Premium Data Transfer

 Source Server         : 172.16.75.105
 Source Server Type    : MySQL
 Source Server Version : 80025 (8.0.25)
 Source Host           : 172.16.75.105:3306
 Source Schema         : springboot-jpa

 Target Server Type    : MySQL
 Target Server Version : 80025 (8.0.25)
 File Encoding         : 65001

 Date: 21/02/2024 18:29:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_person
-- ----------------------------
DROP TABLE IF EXISTS `t_person`;
CREATE TABLE `t_person` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sex` bit(1) DEFAULT NULL,
  `height` double DEFAULT NULL,
  `hover` varchar(255) DEFAULT NULL,
  `self_info` tinytext,
  `creator_id` bigint DEFAULT NULL,
  `created_time` datetime(6) DEFAULT NULL,
  `last_modifier_id` bigint DEFAULT NULL,
  `last_modified_time` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of t_person
-- ----------------------------
BEGIN;
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (1, '张三', 'abcd@123456', b'0', 173.55, '', '这是我个人的一条自我介绍', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (2, '张三1', 'abcd@123456', b'0', 183.55, '[\"篮球\",\"跑酷\",\"打游戏\"]', '您好，我是张三1', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (3, '李四1', 'abcd@123456', b'0', 187.55, '[\"篮球\",\"打游戏\"]', '您好，我是李四1', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (4, '王五', 'abcd@123456', b'0', 187.55, '[\"跑酷\",\"打游戏\"]', '您好，我是王五', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (5, '白鹤先生', '123456', b'1', 163.44, '[\"看书\",\"写诗\"]', '这个家伙很懒，什么也没有留下', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (6, '杨幂', '123456', b'1', 163.44, '[\"拍电视\",\"电影\"]', '我是个演员', 0, '2024-02-21 14:18:24.000000', 0, '2024-02-21 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (7, '李杰', '123456', b'1', 183.44, '[\"编程\",\"赛车\"]', '我是一个程序员', 0, '2024-01-21 14:18:24.000000', 0, '2024-02-20 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (8, '李想', '123456', b'0', 173.74, '[\"创业\",\"造车\"]', '创业者', 0, '2023-02-21 14:18:24.000000', 0, '2024-02-20 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (9, '何小鹏', '123456', b'0', 179.24, '[\"创业\",\"造车\"]', '创业者', 0, '2023-02-21 14:18:24.000000', 0, '2024-01-19 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (10, '余承东', '123456', b'0', 178.11, '[\"创业\",\"造车\"]', '华为', 0, '2020-02-21 14:18:24.000000', 0, '2024-01-19 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (11, '雷军', '123456', b'0', 158.11, '[\"创业\",\"造车\"]', '小米', 0, '2020-08-14 14:18:24.000000', 0, '2024-01-19 14:18:24.000000', b'0');
INSERT INTO `t_person` (`id`, `user_name`, `password`, `sex`, `height`, `hover`, `self_info`, `creator_id`, `created_time`, `last_modifier_id`, `last_modified_time`, `is_deleted`) VALUES (12, '孙少军', '123456', b'0', 185.11, '[\"创业\",\"造车\"]', '小米', 0, '2021-08-14 14:18:24.000000', 0, '2022-06-19 14:18:24.000000', b'0');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
