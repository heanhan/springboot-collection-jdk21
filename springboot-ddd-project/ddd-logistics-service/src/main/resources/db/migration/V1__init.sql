-- 商品与轨迹是发货聚合内的有限快照，JSON 不承担跨聚合关联。
CREATE TABLE t_shipment (
 shipment_id VARCHAR(64) PRIMARY KEY, order_id VARCHAR(64) NOT NULL, order_no VARCHAR(64),
 user_id VARCHAR(64), carrier VARCHAR(16), tracking_no VARCHAR(128), status VARCHAR(16),
 receiver VARCHAR(64), mobile VARCHAR(32), address VARCHAR(512), shipped_at DATETIME(6), delivered_at DATETIME(6),
 items LONGTEXT, tracks LONGTEXT, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_shipment_order(order_id), KEY idx_shipment_status(status,shipped_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
