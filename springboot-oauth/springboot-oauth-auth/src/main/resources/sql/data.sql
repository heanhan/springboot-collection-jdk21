-- ============================================================
-- OAuth2.0 授权中心 - 种子数据脚本
-- ============================================================
USE `oauth_auth`;

-- ==================== 用户数据 ====================
-- admin / admin123
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `gender`, `status`, `create_time`, `update_time`) VALUES
(1001, 'admin', '$2a$10$FZJaZMzUYI5gOg3I7vlM/OC/0yFanhFhU11L8KNB6NFKkfXMieIXm', '超级管理员', '13800000001', 'admin@example.com', NULL, 1, 1, NOW(), NOW()),
(1002, 'user', '$2a$10$FZJaZMzUYI5gOg3I7vlM/OC/0yFanhFhU11L8KNB6NFKkfXMieIXm', '普通用户', '13800000002', 'user@example.com', NULL, 1, 1, NOW(), NOW());

-- ==================== 角色数据 ====================
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `create_time`, `update_time`) VALUES
(2001, '超级管理员', 'ADMIN', '拥有所有权限', 1, NOW(), NOW()),
(2002, '普通用户', 'USER', '仅拥有基本权限', 1, NOW(), NOW());

-- ==================== 权限数据 ====================
INSERT INTO `sys_permission` (`id`, `perm_name`, `perm_code`, `type`, `url`, `method`, `description`, `status`, `create_time`) VALUES
(3001, '查看用户', 'user:list',   3, '/auth/current',     'GET',    '查看用户信息', 1, NOW()),
(3002, '查看菜单', 'menu:list',   3, '/auth/menus',       'GET',    '查看菜单树',   1, NOW()),
(3003, '用户登录', 'auth:login',  3, '/auth/login',       'POST',   '用户登录',     1, NOW()),
(3004, '用户登出', 'auth:logout', 3, '/auth/logout',      'POST',   '用户登出',     1, NOW()),
(3005, '刷新令牌', 'auth:refresh',3, '/auth/refresh',     'POST',   '刷新Token',    1, NOW()),
(3006, '查看角色', 'role:list',   2, NULL,                NULL,     '查看角色列表', 1, NOW()),
(3007, '查看权限', 'perm:list',   2, NULL,                NULL,     '查看权限列表', 1, NOW());

-- ==================== 管理接口权限（type=3，供动态鉴权 DynamicAuthorizationFilter 使用） ====================
INSERT INTO `sys_permission` (`id`, `perm_name`, `perm_code`, `type`, `url`, `method`, `description`, `status`, `create_time`) VALUES
-- 用户管理 /admin/users/**
(3101, '用户管理-列表', 'sys:user:list',   3, '/admin/users',       'GET',              '分页查询用户',   1, NOW()),
(3102, '用户管理-详情', 'sys:user:query',  3, '/admin/users/*',     'GET',              '查询用户详情',   1, NOW()),
(3103, '用户管理-新增', 'sys:user:create', 3, '/admin/users',       'POST',             '新增用户',       1, NOW()),
(3104, '用户管理-更新', 'sys:user:update', 3, '/admin/users',       'PUT',              '更新用户',       1, NOW()),
(3105, '用户管理-删除', 'sys:user:delete', 3, '/admin/users/*',     'DELETE',           '删除用户',       1, NOW()),
(3106, '用户管理-改状态', 'sys:user:status', 3, '/admin/users/*/status',   'PUT',        '修改用户状态',   1, NOW()),
(3107, '用户管理-重置密码', 'sys:user:resetpwd', 3, '/admin/users/*/password', 'PUT',      '重置用户密码',   1, NOW()),
(3108, '用户管理-分配角色', 'sys:user:assign', 3, '/admin/users/*/roles',    'GET,PUT',    '查询/分配用户角色', 1, NOW()),
-- 角色管理 /admin/roles/**
(3201, '角色管理-列表', 'sys:role:list',   3, '/admin/roles',       'GET',              '分页查询角色',   1, NOW()),
(3202, '角色管理-全部', 'sys:role:all',    3, '/admin/roles/all',   'GET',              '查询全部角色',   1, NOW()),
(3203, '角色管理-详情', 'sys:role:query',  3, '/admin/roles/*',     'GET',              '查询角色详情',   1, NOW()),
(3204, '角色管理-新增', 'sys:role:create', 3, '/admin/roles',       'POST',             '新增角色',       1, NOW()),
(3205, '角色管理-更新', 'sys:role:update', 3, '/admin/roles',       'PUT',              '更新角色',       1, NOW()),
(3206, '角色管理-删除', 'sys:role:delete', 3, '/admin/roles/*',     'DELETE',           '删除角色',       1, NOW()),
(3207, '角色管理-分配权限', 'sys:role:assignperm', 3, '/admin/roles/*/permissions', 'GET,PUT', '查询/分配角色权限', 1, NOW()),
(3208, '角色管理-分配菜单', 'sys:role:assignmenu', 3, '/admin/roles/*/menus',       'GET,PUT', '查询/分配角色菜单', 1, NOW()),
-- 权限管理 /admin/permissions/**
(3301, '权限管理-列表', 'sys:perm:list',   3, '/admin/permissions',     'GET',          '分页查询权限',   1, NOW()),
(3302, '权限管理-全部', 'sys:perm:all',    3, '/admin/permissions/all', 'GET',          '查询全部权限',   1, NOW()),
(3303, '权限管理-详情', 'sys:perm:query',  3, '/admin/permissions/*',   'GET',          '查询权限详情',   1, NOW()),
(3304, '权限管理-新增', 'sys:perm:create', 3, '/admin/permissions',     'POST',         '新增权限',       1, NOW()),
(3305, '权限管理-更新', 'sys:perm:update', 3, '/admin/permissions',     'PUT',          '更新权限',       1, NOW()),
(3306, '权限管理-删除', 'sys:perm:delete', 3, '/admin/permissions/*',   'DELETE',       '删除权限',       1, NOW()),
-- 菜单管理 /admin/menus/**
(3401, '菜单管理-列表', 'sys:menu:list',   3, '/admin/menus',       'GET',              '查询全部菜单',   1, NOW()),
(3402, '菜单管理-树',   'sys:menu:tree',   3, '/admin/menus/tree',  'GET',              '查询菜单树',     1, NOW()),
(3403, '菜单管理-详情', 'sys:menu:query',  3, '/admin/menus/*',     'GET',              '查询菜单详情',   1, NOW()),
(3404, '菜单管理-新增', 'sys:menu:create', 3, '/admin/menus',       'POST',             '新增菜单',       1, NOW()),
(3405, '菜单管理-更新', 'sys:menu:update', 3, '/admin/menus',       'PUT',              '更新菜单',       1, NOW()),
(3406, '菜单管理-删除', 'sys:menu:delete', 3, '/admin/menus/*',     'DELETE',           '删除菜单',       1, NOW()),
-- 客户端管理 /admin/clients/**
(3501, '客户端管理-列表', 'sys:client:list',   3, '/admin/clients',   'GET',            '分页查询客户端', 1, NOW()),
(3502, '客户端管理-详情', 'sys:client:query',  3, '/admin/clients/*', 'GET',            '查询客户端详情', 1, NOW()),
(3503, '客户端管理-新增', 'sys:client:create', 3, '/admin/clients',   'POST',           '新增客户端',     1, NOW()),
(3504, '客户端管理-更新', 'sys:client:update', 3, '/admin/clients',   'PUT',            '更新客户端',     1, NOW()),
(3505, '客户端管理-删除', 'sys:client:delete', 3, '/admin/clients/*', 'DELETE',         '删除客户端',     1, NOW());

-- ==================== 菜单数据 ====================
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `icon`, `type`, `permission`, `sort`, `visible`, `status`, `create_time`) VALUES
-- 一级目录
(4001, 0,    '系统管理', '/system',   NULL,           'Setting',  0, NULL,         1, 1, 1, NOW()),
(4002, 0,    '权限管理', '/permission',NULL,          'Lock',      0, NULL,         2, 1, 1, NOW()),
-- 二级菜单 - 系统管理
(4003, 4001, '用户管理', '/system/user',    'system/user/index',    'User',      1, 'user:list',   1, 1, 1, NOW()),
(4004, 4001, '角色管理', '/system/role',    'system/role/index',    'Peoples',   1, 'role:list',   2, 1, 1, NOW()),
(4005, 4001, '菜单管理', '/system/menu',    'system/menu/index',    'TreeTable', 1, 'menu:list',   3, 1, 1, NOW()),
-- 二级菜单 - 权限管理
(4006, 4002, '权限列表', '/permission/list','permission/list/index','Key',       1, 'perm:list',   1, 1, 1, NOW());

-- ==================== 用户-角色关联 ====================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1001, 2001),
(1002, 2002);

-- ==================== 角色-权限关联 ====================
-- ADMIN 角色拥有所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2001, 3001), (2001, 3002), (2001, 3003), (2001, 3004), (2001, 3005), (2001, 3006), (2001, 3007),
-- ADMIN 拥有全部管理接口权限
(2001, 3101), (2001, 3102), (2001, 3103), (2001, 3104), (2001, 3105), (2001, 3106), (2001, 3107), (2001, 3108),
(2001, 3201), (2001, 3202), (2001, 3203), (2001, 3204), (2001, 3205), (2001, 3206), (2001, 3207), (2001, 3208),
(2001, 3301), (2001, 3302), (2001, 3303), (2001, 3304), (2001, 3305), (2001, 3306),
(2001, 3401), (2001, 3402), (2001, 3403), (2001, 3404), (2001, 3405), (2001, 3406),
(2001, 3501), (2001, 3502), (2001, 3503), (2001, 3504), (2001, 3505),
-- USER 角色仅拥有基本权限
(2002, 3001), (2002, 3002), (2002, 3003), (2002, 3004), (2002, 3005);

-- ==================== 角色-菜单关联 ====================
-- ADMIN 角色拥有所有菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2001, 4001), (2001, 4002), (2001, 4003), (2001, 4004), (2001, 4005), (2001, 4006),
-- USER 角色仅有系统管理下的用户管理
(2002, 4001), (2002, 4003);

-- ==================== OAuth 客户端种子数据 ====================
-- client_id = demo-client，client_secret 明文 = admin123（BCrypt 存储，仅供演示）
INSERT INTO `oauth_client` (`id`, `client_id`, `client_secret`, `client_name`, `scopes`, `grant_types`, `access_token_validity`, `status`, `create_time`) VALUES
(5001, 'demo-client', '$2a$10$FZJaZMzUYI5gOg3I7vlM/OC/0yFanhFhU11L8KNB6NFKkfXMieIXm', '演示客户端', 'read,write', 'client_credentials', 3600, 1, NOW());
