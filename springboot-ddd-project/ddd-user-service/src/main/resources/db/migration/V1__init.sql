-- ==============================================================================
-- ddd-user-service Flyway 初始化脚本 V1
-- ==============================================================================
-- 表设计说明：
--   1. 主键统一使用 varchar(32) 存储雪花 ID 字符串（避免 JS 精度丢失）
--   2. 所有表带审计字段：create_time / update_time / version / deleted
--   3. 索引按业务查询模式设计，避免过度索引
-- ==============================================================================

-- ---------------------------------------------------------------------------
-- t_user 用户主表 (User 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user
(
    user_id       VARCHAR(32)  NOT NULL COMMENT '用户 ID (雪花)',
    nickname      VARCHAR(64)  NOT NULL COMMENT '昵称',
    avatar        VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    mobile        VARCHAR(20)  NOT NULL COMMENT '手机号 (唯一)',
    email         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    gender        TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0=未知 1=男 2=女',
    member_level  VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT '会员等级: NORMAL/SILVER/GOLD/DIAMOND',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED',
    register_time DATETIME     NOT NULL COMMENT '注册时间',
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_mobile (mobile, deleted),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表 (User 聚合根)';

-- ---------------------------------------------------------------------------
-- t_user_address 收货地址表 (User 聚合内实体)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_address
(
    address_id VARCHAR(32)  NOT NULL COMMENT '地址 ID',
    user_id    VARCHAR(32)  NOT NULL COMMENT '所属用户 ID',
    receiver   VARCHAR(64)  NOT NULL COMMENT '收件人',
    mobile     VARCHAR(20)  NOT NULL COMMENT '收件人手机号',
    province   VARCHAR(32)  NOT NULL COMMENT '省',
    city       VARCHAR(32)  NOT NULL COMMENT '市',
    district   VARCHAR(32)  NOT NULL COMMENT '区/县',
    detail     VARCHAR(255) NOT NULL COMMENT '详细地址',
    zip_code   VARCHAR(10)  DEFAULT NULL COMMENT '邮编',
    tag        VARCHAR(16)  DEFAULT NULL COMMENT '标签: HOME/COMPANY/OTHER',
    is_default TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0=否 1=是',
    create_time DATETIME    NOT NULL,
    update_time DATETIME    NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (address_id),
    KEY idx_user_id (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址 (User 聚合内实体)';

-- ---------------------------------------------------------------------------
-- t_role 角色表 (Role 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_role
(
    role_id     VARCHAR(32) NOT NULL COMMENT '角色 ID',
    role_code   VARCHAR(64) NOT NULL COMMENT '角色码: USER/ADMIN/MERCHANT',
    role_name   VARCHAR(64) NOT NULL COMMENT '角色名',
    description VARCHAR(255) DEFAULT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time DATETIME    NOT NULL,
    update_time DATETIME    NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表 (RBAC)';

-- ---------------------------------------------------------------------------
-- t_permission 权限表 (Permission 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_permission
(
    permission_id   VARCHAR(32)  NOT NULL,
    permission_code VARCHAR(128) NOT NULL COMMENT '权限码: order:create / product:read',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名',
    resource        VARCHAR(64)  NOT NULL COMMENT '资源: order/product/user',
    action          VARCHAR(32)  NOT NULL COMMENT '动作: create/read/update/delete',
    description     VARCHAR(255) DEFAULT NULL,
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_code (permission_code, deleted),
    KEY idx_resource (resource)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表 (RBAC)';

-- ---------------------------------------------------------------------------
-- t_user_role 用户-角色关联
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_role
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     VARCHAR(32) NOT NULL,
    role_id     VARCHAR(32) NOT NULL,
    create_time DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联';

-- ---------------------------------------------------------------------------
-- t_role_permission 角色-权限关联
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_role_permission
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    role_id       VARCHAR(32) NOT NULL,
    permission_id VARCHAR(32) NOT NULL,
    create_time   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联';

-- ==============================================================================
-- 初始化数据
-- ==============================================================================

-- 角色
INSERT INTO t_role (role_id, role_code, role_name, description, status, create_time, update_time, version, deleted)
VALUES ('1', 'USER', '普通用户', '注册用户默认角色', 'ACTIVE', NOW(), NOW(), 0, 0),
       ('2', 'ADMIN', '管理员', '拥有所有权限', 'ACTIVE', NOW(), NOW(), 0, 0),
       ('3', 'MERCHANT', '商家', '管理自己店铺的商品与订单', 'ACTIVE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 权限
INSERT INTO t_permission (permission_id, permission_code, permission_name, resource, action, create_time, update_time, version, deleted)
VALUES ('100', 'user:read',      '查看用户资料',   'user',      'read',   NOW(), NOW(), 0, 0),
       ('101', 'user:update',    '修改用户资料',   'user',      'update', NOW(), NOW(), 0, 0),
       ('102', 'address:manage', '管理收货地址',   'address',   'manage', NOW(), NOW(), 0, 0),
       ('200', 'product:read',   '查看商品',       'product',   'read',   NOW(), NOW(), 0, 0),
       ('201', 'product:manage', '管理商品',       'product',   'manage', NOW(), NOW(), 0, 0),
       ('300', 'order:create',   '创建订单',       'order',     'create', NOW(), NOW(), 0, 0),
       ('301', 'order:read',     '查看订单',       'order',     'read',   NOW(), NOW(), 0, 0),
       ('302', 'order:cancel',   '取消订单',       'order',     'cancel', NOW(), NOW(), 0, 0),
       ('400', 'payment:pay',    '支付订单',       'payment',   'pay',    NOW(), NOW(), 0, 0),
       ('401', 'payment:refund', '申请退款',       'payment',   'refund', NOW(), NOW(), 0, 0),
       ('500', 'cart:manage',    '管理购物车',     'cart',      'manage', NOW(), NOW(), 0, 0),
       ('900', 'admin:all',      '管理员全权限',   'admin',     'all',    NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 普通用户角色权限
INSERT INTO t_role_permission (role_id, permission_id, create_time)
VALUES ('1', '100', NOW()), ('1', '101', NOW()), ('1', '102', NOW()),
       ('1', '200', NOW()), ('1', '300', NOW()), ('1', '301', NOW()), ('1', '302', NOW()),
       ('1', '400', NOW()), ('1', '401', NOW()), ('1', '500', NOW())
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 管理员全部权限
INSERT INTO t_role_permission (role_id, permission_id, create_time)
SELECT '2', permission_id, NOW() FROM t_permission
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 商家角色权限
INSERT INTO t_role_permission (role_id, permission_id, create_time)
VALUES ('3', '100', NOW()), ('3', '200', NOW()), ('3', '201', NOW()), ('3', '301', NOW())
ON DUPLICATE KEY UPDATE create_time = NOW();
