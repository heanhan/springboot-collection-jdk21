-- =============================================================================
-- 租户生命周期、租户级 RBAC、数据源状态与审计字段
-- =============================================================================

ALTER TABLE `sys_tenant`
    ADD COLUMN `tenant_code` varchar(64) NULL COMMENT '租户编码' AFTER `id`,
    ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '租户状态：PENDING/ACTIVE/DISABLED/DESTROYED' AFTER `logo_url`,
    ADD COLUMN `expires_at` datetime NULL COMMENT '租户到期时间' AFTER `status`,
    ADD COLUMN `provision_status` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '开通状态：PENDING/SUCCESS/FAILED' AFTER `expires_at`,
    ADD COLUMN `database_name` varchar(128) NULL COMMENT '租户数据库名' AFTER `provision_status`,
    ADD COLUMN `schema_version` int NOT NULL DEFAULT 0 COMMENT '租户库 schema 版本' AFTER `database_name`,
    ADD COLUMN `provision_error` varchar(500) NULL COMMENT '最近一次开通失败原因' AFTER `schema_version`,
    ADD COLUMN `destroyed_at` datetime NULL COMMENT '销毁时间' AFTER `provision_error`;

UPDATE `sys_tenant`
SET `tenant_code` = CONCAT('TENANT_', LPAD(`id`, 6, '0')),
    `status` = 'ACTIVE',
    `provision_status` = 'SUCCESS',
    `expires_at` = DATE_ADD(NOW(), INTERVAL 1 YEAR)
WHERE `tenant_code` IS NULL;

ALTER TABLE `sys_tenant`
    MODIFY COLUMN `tenant_code` varchar(64) NOT NULL COMMENT '租户编码',
    ADD UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    ADD KEY `idx_tenant_status` (`status`, `is_del`);

ALTER TABLE `sys_tenant_data`
    ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'READY' COMMENT '数据源状态：PROVISIONING/READY/DISABLED/ERROR' AFTER `password`,
    ADD COLUMN `schema_version` int NOT NULL DEFAULT 0 COMMENT '租户库 schema 版本' AFTER `status`,
    ADD COLUMN `password_cipher_version` int NOT NULL DEFAULT 0 COMMENT '密码加密版本：0=明文，1=RSA' AFTER `schema_version`,
    ADD COLUMN `last_check_at` datetime NULL COMMENT '最近一次连通检查时间' AFTER `password_cipher_version`;

UPDATE `sys_tenant_data`
SET `status` = IF(`is_del` = 1, 'DISABLED', 'READY'),
    `schema_version` = 1;

ALTER TABLE `sys_tenant_data`
    ADD KEY `idx_tenant_data_status` (`status`, `is_del`);

ALTER TABLE `sys_role`
    ADD COLUMN `tenant_id` int NOT NULL DEFAULT 0 COMMENT '角色所属租户，0=平台角色' AFTER `id`,
    ADD COLUMN `role_code` varchar(100) NULL COMMENT '角色编码' AFTER `role_name`;

UPDATE `sys_role`
SET `role_code` = `role_name`
WHERE `role_code` IS NULL OR `role_code` = '';

ALTER TABLE `sys_role`
    MODIFY COLUMN `role_code` varchar(100) NOT NULL COMMENT '角色编码',
    ADD UNIQUE KEY `uk_role_tenant_code` (`tenant_id`, `role_code`),
    ADD KEY `idx_role_tenant` (`tenant_id`, `is_del`);

ALTER TABLE `sys_user`
    ADD COLUMN `token_version` int NOT NULL DEFAULT 0 COMMENT 'Token 版本，修改密码/停用/角色变更时递增' AFTER `status`;

ALTER TABLE `sys_user`
    ADD UNIQUE KEY `uk_user_tenant_username` (`tenant_id`, `username`),
    ADD KEY `idx_user_tenant_status` (`tenant_id`, `status`);

ALTER TABLE `sys_menu`
    MODIFY COLUMN `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0=正常，1=删除',
    MODIFY COLUMN `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE `sys_auth_node`
    ADD COLUMN `permission_code` varchar(100) NULL COMMENT '权限编码' AFTER `method`;

UPDATE `sys_auth_node` SET `permission_code` = 'tenant-business:read' WHERE `id` = 1;
UPDATE `sys_auth_node` SET `permission_code` = 'user:create' WHERE `id` = 2;
UPDATE `sys_auth_node` SET `permission_code` = 'datasource:read' WHERE `id` = 3;

INSERT IGNORE INTO `sys_auth_node`
    (`parent_id`, `name`, `path`, `method`, `list_order`, `is_del`, `create_time`, `permission_code`)
VALUES
    (0, '查看当前用户', '/api/me', 'GET', 10, 0, NOW(), 'profile:read'),
    (0, '查看当前菜单', '/api/menus', 'GET', 11, 0, NOW(), 'profile:read'),
    (0, '查看当前权限', '/api/permissions', 'GET', 12, 0, NOW(), 'profile:read'),
    (0, '修改当前密码', '/api/password/change', 'POST', 13, 0, NOW(), 'profile:update'),
    (0, '查询租户列表', '/api/tenants', 'GET', 20, 0, NOW(), 'tenant:read'),
    (0, '查询租户详情', '/api/tenants/{id}', 'GET', 21, 0, NOW(), 'tenant:read'),
    (0, '创建租户', '/api/tenants', 'POST', 22, 0, NOW(), 'tenant:create'),
    (0, '修改租户', '/api/tenants/{id}', 'PUT', 23, 0, NOW(), 'tenant:update'),
    (0, '续期租户', '/api/tenants/{id}/renew', 'POST', 24, 0, NOW(), 'tenant:renew'),
    (0, '停用租户', '/api/tenants/{id}/disable', 'POST', 25, 0, NOW(), 'tenant:disable'),
    (0, '恢复租户', '/api/tenants/{id}/enable', 'POST', 26, 0, NOW(), 'tenant:enable'),
    (0, '销毁租户', '/api/tenants/{id}/destroy', 'POST', 27, 0, NOW(), 'tenant:destroy'),
    (0, '重试租户开通', '/api/tenants/{id}/retry-provision', 'POST', 28, 0, NOW(), 'tenant:retry'),
    (0, '查看租户开通任务', '/api/tenants/{id}/tasks', 'GET', 29, 0, NOW(), 'tenant:read'),
    (0, '查看租户数据源', '/api/tenants/{id}/datasource', 'GET', 30, 0, NOW(), 'datasource:read'),
    (0, '测试租户数据源', '/api/tenants/{id}/datasource/test', 'POST', 31, 0, NOW(), 'datasource:test'),
    (0, '查询租户成员', '/api/tenants/{tenantId}/users', 'GET', 40, 0, NOW(), 'user:read'),
    (0, '创建租户成员', '/api/tenants/{tenantId}/users', 'POST', 41, 0, NOW(), 'user:create'),
    (0, '兼容创建租户成员', '/api/addTenantInfo', 'POST', 46, 0, NOW(), 'user:create'),
    (0, '修改租户成员', '/api/tenants/{tenantId}/users/{userId}', 'PUT', 42, 0, NOW(), 'user:update'),
    (0, '停用租户成员', '/api/tenants/{tenantId}/users/{userId}/disable', 'POST', 43, 0, NOW(), 'user:disable'),
    (0, '重置成员密码', '/api/tenants/{tenantId}/users/{userId}/reset-password', 'POST', 44, 0, NOW(), 'user:reset-password'),
    (0, '分配成员角色', '/api/tenants/{tenantId}/users/{userId}/roles', 'PUT', 45, 0, NOW(), 'user:assign-role'),
    (0, '查询角色', '/api/roles', 'GET', 50, 0, NOW(), 'role:read'),
    (0, '创建角色', '/api/roles', 'POST', 51, 0, NOW(), 'role:create'),
    (0, '修改角色', '/api/roles/{id}', 'PUT', 52, 0, NOW(), 'role:update'),
    (0, '分配角色权限', '/api/roles/{id}/permissions', 'PUT', 53, 0, NOW(), 'role:assign-permission'),
    (0, '删除角色', '/api/roles/{id}', 'DELETE', 54, 0, NOW(), 'role:delete'),
    (0, '新增租户业务数据', '/api/test/**', 'POST', 60, 0, NOW(), 'tenant-business:write'),
    (0, '修改租户业务数据', '/api/test/**', 'PUT', 61, 0, NOW(), 'tenant-business:write'),
    (0, '删除租户业务数据', '/api/test/**', 'DELETE', 62, 0, NOW(), 'tenant-business:write'),
    (0, '查看系统指标', '/actuator/metrics', 'GET', 70, 0, NOW(), 'system:monitor'),
    (0, '查看系统指标详情', '/actuator/metrics/**', 'GET', 71, 0, NOW(), 'system:monitor'),
    (0, '查看 Prometheus 指标', '/actuator/prometheus', 'GET', 72, 0, NOW(), 'system:monitor'),
    (0, '查看应用信息', '/actuator/info', 'GET', 73, 0, NOW(), 'system:monitor'),
    (0, '查询权限节点', '/api/auth-nodes', 'GET', 80, 0, NOW(), 'role:read'),
    (0, '新增权限节点', '/api/auth-nodes', 'POST', 81, 0, NOW(), 'role:assign-permission'),
    (0, '修改权限节点', '/api/auth-nodes/{id}', 'PUT', 82, 0, NOW(), 'role:assign-permission'),
    (0, '删除权限节点', '/api/auth-nodes/{id}', 'DELETE', 83, 0, NOW(), 'role:assign-permission');

ALTER TABLE `sys_auth_node`
    MODIFY COLUMN `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    ADD KEY `idx_auth_node_permission` (`permission_code`),
    ADD KEY `idx_auth_node_path` (`path`, `method`);

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`permission_code`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限定义表';

INSERT IGNORE INTO `sys_permission` (`permission_code`, `permission_name`)
SELECT DISTINCT `permission_code`, `name` FROM `sys_auth_node` WHERE `is_del` = 0;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` int UNSIGNED NOT NULL COMMENT '用户id',
    `role_id` int UNSIGNED NOT NULL COMMENT '角色id',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关系表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` int UNSIGNED NOT NULL COMMENT '角色id',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_code`),
    KEY `idx_role_permission_role` (`role_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色权限关系表';

CREATE TABLE IF NOT EXISTS `sys_provisioning_task` (
    `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务id',
    `tenant_id` int UNSIGNED NOT NULL COMMENT '租户id',
    `task_type` varchar(30) NOT NULL COMMENT '任务类型：CREATE/RETRY/DESTROY',
    `status` varchar(20) NOT NULL COMMENT '任务状态：RUNNING/SUCCESS/FAILED',
    `current_step` varchar(50) NULL COMMENT '当前步骤',
    `error_message` varchar(500) NULL COMMENT '失败原因',
    `start_time` datetime NOT NULL COMMENT '开始时间',
    `finish_time` datetime NULL COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_provisioning_tenant` (`tenant_id`, `start_time`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户开通与销毁任务表';

INSERT IGNORE INTO `sys_role`
    (`tenant_id`, `role_code`, `role_name`, `description`, `parent_id`, `create_time`, `is_del`)
VALUES
    (0, 'ROLE_PLATFORM_ADMIN', '平台管理员', '管理全部租户、数据源、用户和角色', 0, NOW(), 0);

INSERT INTO `sys_role`
    (`tenant_id`, `role_code`, `role_name`, `description`, `parent_id`, `create_time`, `is_del`)
SELECT t.`id`, 'ROLE_TENANT_ADMIN', '租户管理员', '管理当前租户的成员和业务数据', 0, NOW(), 0
FROM `sys_tenant` t
WHERE t.`id` > 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_code` = 'ROLE_TENANT_ADMIN'
  );

INSERT INTO `sys_role`
    (`tenant_id`, `role_code`, `role_name`, `description`, `parent_id`, `create_time`, `is_del`)
SELECT t.`id`, 'ROLE_TENANT_MEMBER', '租户成员', '访问当前租户的授权业务数据', 0, NOW(), 0
FROM `sys_tenant` t
WHERE t.`id` > 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_code` = 'ROLE_TENANT_MEMBER'
  );

SET @platform_role_id = (SELECT `id` FROM `sys_role` WHERE `tenant_id` = 0 AND `role_code` = 'ROLE_PLATFORM_ADMIN' LIMIT 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`,
       CASE
           WHEN u.`tenant_id` = 0 AND u.`role_id` = 2 THEN @platform_role_id
           WHEN u.`tenant_id` > 0 AND u.`role_id` = 2 THEN ra.`id`
           ELSE rm.`id`
       END
FROM `sys_user` u
LEFT JOIN `sys_role` ra ON ra.`tenant_id` = u.`tenant_id` AND ra.`role_code` = 'ROLE_TENANT_ADMIN'
LEFT JOIN `sys_role` rm ON rm.`tenant_id` = u.`tenant_id` AND rm.`role_code` = 'ROLE_TENANT_MEMBER'
WHERE u.`status` = 1;

UPDATE `sys_user` u
LEFT JOIN `sys_role` ra ON ra.`tenant_id` = u.`tenant_id` AND ra.`role_code` = 'ROLE_TENANT_ADMIN'
LEFT JOIN `sys_role` rm ON rm.`tenant_id` = u.`tenant_id` AND rm.`role_code` = 'ROLE_TENANT_MEMBER'
SET u.`role_id` = CASE
    WHEN u.`tenant_id` = 0 AND u.`role_id` = 2 THEN @platform_role_id
    WHEN u.`tenant_id` > 0 AND u.`role_id` = 2 THEN ra.`id`
    ELSE rm.`id`
END
WHERE u.`status` = 1;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_code`)
SELECT @platform_role_id, n.`permission_code`
FROM `sys_auth_node` n
WHERE n.`is_del` = 0;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_code`)
SELECT r.`id`, n.`permission_code`
FROM `sys_role` r
JOIN `sys_auth_node` n ON n.`permission_code` IN (
    'profile:read', 'profile:update', 'tenant:read', 'tenant:update', 'tenant:renew',
    'datasource:read', 'datasource:test', 'user:read', 'user:create',
    'user:update', 'user:disable', 'user:reset-password', 'user:assign-role',
    'role:read', 'role:create', 'role:update', 'role:assign-permission', 'role:delete',
    'tenant-business:read', 'tenant-business:write'
)
WHERE r.`tenant_id` > 0 AND r.`role_code` = 'ROLE_TENANT_ADMIN' AND n.`is_del` = 0;

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_code`)
SELECT r.`id`, n.`permission_code`
FROM `sys_role` r
JOIN `sys_auth_node` n ON n.`permission_code` IN (
    'profile:read', 'profile:update', 'tenant:read', 'tenant-business:read', 'tenant-business:write'
)
WHERE r.`tenant_id` > 0 AND r.`role_code` = 'ROLE_TENANT_MEMBER' AND n.`is_del` = 0;

UPDATE `sys_auth_node` SET `permission_code` = UPPER(`permission_code`) WHERE `is_del` = 0;
UPDATE `sys_role_permission` SET `permission_code` = UPPER(`permission_code`);
