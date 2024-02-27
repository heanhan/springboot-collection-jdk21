/*
 Navicat Premium Data Transfer

 Source Server         : 172.16.75.105
 Source Server Type    : MySQL
 Source Server Version : 80025 (8.0.25)
 Source Host           : 172.16.75.105:3306
 Source Schema         : springboot-redission

 Target Server Type    : MySQL
 Target Server Version : 80025 (8.0.25)
 File Encoding         : 65001

 Date: 27/02/2024 18:00:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_storage
-- ----------------------------
DROP TABLE IF EXISTS `t_storage`;
CREATE TABLE `t_storage` (
  `id` int NOT NULL AUTO_INCREMENT,
  `number` bigint DEFAULT NULL,
  `product` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of t_storage
-- ----------------------------
BEGIN;
INSERT INTO `t_storage` (`id`, `number`, `product`, `version`) VALUES (1, 19, 'iphone 15 pro max', 'ios 17.1');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
