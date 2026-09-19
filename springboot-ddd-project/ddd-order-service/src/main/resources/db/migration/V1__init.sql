-- ==============================================================================
-- ddd-order-service Flyway 初始化脚本 V1
-- ==============================================================================
-- 表设计说明：
--   1. t_order 订单主表 (Order 聚合根)，含金额快照 + 收货地址快照 + 状态时间戳
--   2. t_order_item 订单项 (OrderItem 实体)，随 Order 聚合一起加载
--   3. 金额一律用 DECIMAL(12,2)，禁止 float/double
--   4. idx_status_expire 支撑"超时未支付自动关单"的定时扫描
--   5. 收货地址、商品名、单价均做"快照"冗余：下单后商品改名/改价、用户改地址都不影响历史订单
-- ==============================================================================

-- ---------------------------------------------------------------------------
-- t_order 订单主表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order
(
    order_id         VARCHAR(32)    NOT NULL,
    order_no         VARCHAR(32)    NOT NULL COMMENT '对用户展示的订单号',
    user_id          VARCHAR(32)    NOT NULL,
    status           VARCHAR(16)    NOT NULL COMMENT 'CREATED/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDING/REFUNDED',
    total_amount     DECIMAL(12, 2) NOT NULL COMMENT '商品总额',
    shipping_fee     DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    discount_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    pay_amount       DECIMAL(12, 2) NOT NULL COMMENT '实付 = total + shipping - discount',
    receiver         VARCHAR(64)    NOT NULL COMMENT '收件人快照',
    receiver_mobile  VARCHAR(20)    NOT NULL COMMENT '收件人手机号快照',
    shipping_province VARCHAR(32)   NOT NULL,
    shipping_city    VARCHAR(32)    NOT NULL,
    shipping_district VARCHAR(32)   NOT NULL,
    shipping_detail  VARCHAR(255)   NOT NULL,
    shipping_zipcode VARCHAR(16)    DEFAULT NULL,
    remark           VARCHAR(255)   DEFAULT NULL COMMENT '用户下单备注',
    cancel_reason    VARCHAR(255)   DEFAULT NULL,
    payment_channel  VARCHAR(16)    DEFAULT NULL COMMENT '支付渠道快照',
    payment_trade_no VARCHAR(64)    DEFAULT NULL COMMENT '支付流水号快照',
    created_at       DATETIME       NOT NULL COMMENT '下单时间',
    expire_at        DATETIME       NOT NULL COMMENT '支付超时时间',
    paid_at          DATETIME       DEFAULT NULL,
    shipped_at       DATETIME       DEFAULT NULL,
    completed_at     DATETIME       DEFAULT NULL,
    cancelled_at     DATETIME       DEFAULT NULL,
    create_time      DATETIME       NOT NULL,
    update_time      DATETIME       NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0,
    deleted          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no, deleted),
    KEY idx_user_status (user_id, status, deleted),
    KEY idx_status_expire (status, expire_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单主表';

-- ---------------------------------------------------------------------------
-- t_order_item 订单项
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order_item
(
    item_id     VARCHAR(32)    NOT NULL,
    order_id    VARCHAR(32)    NOT NULL,
    sku_id      VARCHAR(32)    NOT NULL,
    spu_id      VARCHAR(32)    NOT NULL,
    spu_name    VARCHAR(128)   NOT NULL COMMENT '商品名快照',
    sku_name    VARCHAR(255)   NOT NULL COMMENT 'SKU 名快照',
    image       VARCHAR(255)   DEFAULT NULL,
    unit_price  DECIMAL(12, 2) NOT NULL COMMENT '下单时单价快照',
    quantity    INT            NOT NULL,
    subtotal    DECIMAL(12, 2) NOT NULL COMMENT 'unit_price * quantity',
    create_time DATETIME       NOT NULL,
    update_time DATETIME       NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    deleted     TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id),
    KEY idx_order_id (order_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单项';
