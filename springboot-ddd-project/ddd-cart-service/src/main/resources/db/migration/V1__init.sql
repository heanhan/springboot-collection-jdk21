-- 购物车是有限大小聚合，条目 JSON 快照统一参与用户级事务。
CREATE TABLE t_cart (
 user_id VARCHAR(64) PRIMARY KEY, items LONGTEXT NOT NULL, version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
