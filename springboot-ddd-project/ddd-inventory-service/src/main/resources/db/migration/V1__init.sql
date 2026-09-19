-- ==============================================================================
-- ddd-inventory-service Flyway 初始化脚本 V1
-- ==============================================================================
-- 表设计说明：
--   1. t_stock 是热点表，写入频繁，version 字段用于乐观锁
--   2. t_stock_transaction 追加式流水，用于幂等 + 审计
--   3. biz_no 建立唯一索引，同一 bizNo + type 的操作保证幂等
-- ==============================================================================

-- ---------------------------------------------------------------------------
-- t_warehouse 仓库表 (Warehouse 聚合根)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_warehouse
(
    warehouse_id VARCHAR(32)  NOT NULL,
    code         VARCHAR(32)  NOT NULL COMMENT '仓库编码，如 WH-BJ-01',
    name         VARCHAR(64)  NOT NULL COMMENT '仓库名',
    province     VARCHAR(32)  DEFAULT NULL,
    city         VARCHAR(32)  DEFAULT NULL,
    district     VARCHAR(32)  DEFAULT NULL,
    address      VARCHAR(255) DEFAULT NULL,
    contact      VARCHAR(64)  DEFAULT NULL,
    phone        VARCHAR(20)  DEFAULT NULL,
    priority     INT          NOT NULL DEFAULT 0 COMMENT '发货优先级，数字越小越优先',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time  DATETIME     NOT NULL,
    update_time  DATETIME     NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (warehouse_id),
    UNIQUE KEY uk_code (code, deleted),
    KEY idx_priority (priority, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='仓库';

-- ---------------------------------------------------------------------------
-- t_stock 库存表 (Stock 聚合根)
-- ---------------------------------------------------------------------------
-- 关键设计：
--   available_qty：可售库存
--   locked_qty：已预占（下单未支付）
--   total = available + locked
--   version：乐观锁字段，避免"超卖"
CREATE TABLE IF NOT EXISTS t_stock
(
    stock_id      VARCHAR(32) NOT NULL,
    warehouse_id  VARCHAR(32) NOT NULL,
    sku_id        VARCHAR(32) NOT NULL,
    available_qty INT         NOT NULL DEFAULT 0 COMMENT '可售库存',
    locked_qty    INT         NOT NULL DEFAULT 0 COMMENT '预占库存',
    warn_qty      INT         NOT NULL DEFAULT 10 COMMENT '预警阈值',
    create_time   DATETIME    NOT NULL,
    update_time   DATETIME    NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (stock_id),
    UNIQUE KEY uk_wh_sku (warehouse_id, sku_id, deleted),
    KEY idx_sku (sku_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存 (含乐观锁 version)';

-- ---------------------------------------------------------------------------
-- t_stock_transaction 库存流水（追加式，不可修改）
-- ---------------------------------------------------------------------------
-- 关键设计：
--   biz_no + type 建立唯一索引，保证同一业务号同一类型只处理一次（幂等）
CREATE TABLE IF NOT EXISTS t_stock_transaction
(
    transaction_id VARCHAR(32)  NOT NULL,
    biz_no         VARCHAR(64)  NOT NULL COMMENT '业务号（通常是 orderId）',
    type           VARCHAR(16)  NOT NULL COMMENT 'LOCK/UNLOCK/DEDUCT/ROLLBACK/ADJUST',
    warehouse_id   VARCHAR(32)  NOT NULL,
    sku_id         VARCHAR(32)  NOT NULL,
    quantity       INT          NOT NULL COMMENT '变动数量，正数',
    before_qty     INT          NOT NULL COMMENT '变动前 available_qty',
    after_qty      INT          NOT NULL COMMENT '变动后 available_qty',
    before_locked  INT          NOT NULL,
    after_locked   INT          NOT NULL,
    reason         VARCHAR(255) DEFAULT NULL,
    create_time    DATETIME     NOT NULL,
    PRIMARY KEY (transaction_id),
    UNIQUE KEY uk_bizno_type_sku (biz_no, type, sku_id, warehouse_id),
    KEY idx_biz_no (biz_no),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存流水 (追加式)';

-- ==============================================================================
-- 初始化数据：一个默认仓库 + 若干 SKU 库存
-- ==============================================================================
INSERT INTO t_warehouse (warehouse_id, code, name, province, city, address, priority, status, create_time, update_time, version, deleted)
VALUES ('1', 'WH-DEFAULT', '默认仓库', '北京市', '北京市', '朝阳区示例路 1 号', 1, 'ACTIVE', NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();

-- iPhone 15 Pro 两个 SKU 的库存
INSERT INTO t_stock (stock_id, warehouse_id, sku_id, available_qty, locked_qty, warn_qty, create_time, update_time, version, deleted)
VALUES ('10', '1', '2000', 100, 0, 10, NOW(), NOW(), 0, 0),
       ('11', '1', '2001', 50, 0, 5, NOW(), NOW(), 0, 0)
ON DUPLICATE KEY UPDATE update_time = NOW();
