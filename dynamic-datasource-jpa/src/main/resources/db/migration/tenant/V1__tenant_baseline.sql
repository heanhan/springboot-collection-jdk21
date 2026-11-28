-- 每个租户库独立执行的基线迁移
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `t_test` (
    `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) NULL DEFAULT NULL COMMENT '名称',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户业务示例表';
