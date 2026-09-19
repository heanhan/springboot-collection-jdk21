-- ==============================================================================
-- ddd-product-service Flyway 初始化脚本 V1
-- ==============================================================================
-- 表设计说明：
--   1. SPU / SKU 分表，SKU 冗余 spu_name 便于列表展示与订单快照
--   2. Category 采用 parent_id + level 存储树形，通过 code_path 支持子树查询
--   3. Brand 单独聚合，与 Category 通过 SPU 关联
-- ==============================================================================

-- ---------------------------------------------------------------------------
-- t_spu 商品主表 (Spu 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_spu
(
    spu_id      VARCHAR(32)  NOT NULL COMMENT 'SPU ID (雪花)',
    name        VARCHAR(128) NOT NULL COMMENT 'SPU 名称',
    subtitle    VARCHAR(255) DEFAULT NULL COMMENT '副标题',
    brand_id    VARCHAR(32)  NOT NULL COMMENT '品牌 ID',
    category_id VARCHAR(32)  NOT NULL COMMENT '类目 ID',
    main_image  VARCHAR(255) DEFAULT NULL COMMENT '主图 URL',
    album_json  TEXT         DEFAULT NULL COMMENT '相册 JSON 数组',
    detail      MEDIUMTEXT   DEFAULT NULL COMMENT '详情 HTML',
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ON_SALE/OFF_SHELF',
    sales_count BIGINT       NOT NULL DEFAULT 0 COMMENT '累计销量（异步统计）',
    publish_time DATETIME    DEFAULT NULL COMMENT '首次上架时间',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (spu_id),
    KEY idx_category_status (category_id, status, deleted),
    KEY idx_brand (brand_id, deleted),
    KEY idx_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SPU 商品主表';

-- ---------------------------------------------------------------------------
-- t_sku 库存单位表 (Spu 聚合内实体)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_sku
(
    sku_id      VARCHAR(32)   NOT NULL COMMENT 'SKU ID',
    spu_id      VARCHAR(32)   NOT NULL COMMENT '所属 SPU ID',
    spu_name    VARCHAR(128)  NOT NULL COMMENT 'SPU 名称冗余（订单快照）',
    sku_name    VARCHAR(255)  NOT NULL COMMENT 'SKU 名称，例如 "iPhone 15 Pro 256G 黑"',
    spec_json   VARCHAR(512)  DEFAULT NULL COMMENT '规格 JSON, {"颜色":"黑","内存":"256G"}',
    image       VARCHAR(255)  DEFAULT NULL COMMENT 'SKU 主图',
    price       DECIMAL(12, 2) NOT NULL COMMENT '售价（元）',
    market_price DECIMAL(12, 2) DEFAULT NULL COMMENT '市场价（划线价）',
    cost_price  DECIMAL(12, 2) DEFAULT NULL COMMENT '成本价（内部）',
    sku_code    VARCHAR(64)   DEFAULT NULL COMMENT '商家编码',
    barcode     VARCHAR(64)   DEFAULT NULL COMMENT '条形码',
    status      VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/OFF_SHELF',
    weight_gram INT           DEFAULT NULL COMMENT '重量（克）',
    create_time DATETIME      NOT NULL,
    update_time DATETIME      NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (sku_id),
    UNIQUE KEY uk_sku_code (sku_code, deleted),
    KEY idx_spu (spu_id, deleted),
    KEY idx_price (price)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SKU 库存单位';

-- ---------------------------------------------------------------------------
-- t_category 类目表 (Category 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_category
(
    category_id VARCHAR(32)  NOT NULL,
    parent_id   VARCHAR(32)  NOT NULL DEFAULT '0' COMMENT '父类目 ID, 0 表示根',
    code_path   VARCHAR(255) NOT NULL COMMENT '祖先路径, "0/1/5/" 便于子树查询',
    name        VARCHAR(64)  NOT NULL COMMENT '类目名',
    level       INT          NOT NULL COMMENT '层级 1=一级',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    icon        VARCHAR(255) DEFAULT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id),
    KEY idx_parent (parent_id, deleted),
    KEY idx_code_path (code_path)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品类目 (树形)';

-- ---------------------------------------------------------------------------
-- t_brand 品牌表 (Brand 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_brand
(
    brand_id    VARCHAR(32)  NOT NULL,
    name        VARCHAR(64)  NOT NULL COMMENT '品牌名',
    logo        VARCHAR(255) DEFAULT NULL,
    story       TEXT         DEFAULT NULL COMMENT '品牌故事',
    first_letter CHAR(1)     DEFAULT NULL COMMENT '首字母，A-Z 索引用',
    sort        INT          NOT NULL DEFAULT 0,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (brand_id),
    UNIQUE KEY uk_name (name, deleted),
    KEY idx_first_letter (first_letter)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='品牌';

-- ==============================================================================
-- 初始化数据
-- ==============================================================================

-- 类目：手机数码 / 家用电器
INSERT INTO t_category (category_id, parent_id, code_path, name, level, sort, status, create_time, update_time, version, deleted)
VALUES ('1', '0', '0/1/', '手机数码', 1, 10, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('2', '0', '0/2/', '家用电器', 1, 20, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('11', '1', '0/1/11/', '手机', 2, 1, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('12', '1', '0/1/12/', '平板电脑', 2, 2, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('13', '1', '0/1/13/', '智能手表', 2, 3, 'ACTIVE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 品牌：Apple / Xiaomi / Huawei
INSERT INTO t_brand (brand_id, name, logo, first_letter, sort, status, create_time, update_time, version, deleted)
VALUES ('100', 'Apple', NULL, 'A', 1, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('101', 'Xiaomi', NULL, 'X', 2, 'ACTIVE', NOW(), NOW(), 0, 0),
       ('102', 'Huawei', NULL, 'H', 3, 'ACTIVE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 示例 SPU + SKU：iPhone 15 Pro
INSERT INTO t_spu (spu_id, name, subtitle, brand_id, category_id, main_image, status, publish_time, create_time, update_time, version, deleted)
VALUES ('1000', 'iPhone 15 Pro', '钛金属 · A17 Pro 芯片', '100', '11', NULL, 'ON_SALE', NOW(), NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO t_sku (sku_id, spu_id, spu_name, sku_name, spec_json, price, market_price, sku_code, status, create_time, update_time, version, deleted)
VALUES ('2000', '1000', 'iPhone 15 Pro', 'iPhone 15 Pro 256G 原色钛金属',
        '{"颜色":"原色钛金属","内存":"256G"}', 8999.00, 9999.00, 'IP15P-256-NAT', 'ON_SALE', NOW(), NOW(), 0, 0),
       ('2001', '1000', 'iPhone 15 Pro', 'iPhone 15 Pro 512G 蓝色钛金属',
        '{"颜色":"蓝色钛金属","内存":"512G"}', 10999.00, 11999.00, 'IP15P-512-BLU', 'ON_SALE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
