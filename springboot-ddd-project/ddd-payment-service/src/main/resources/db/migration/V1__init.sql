-- 支付与退款采用独立聚合；唯一业务键兜底重复消息。
CREATE TABLE t_payment (
 payment_id VARCHAR(64) PRIMARY KEY, order_id VARCHAR(64) NOT NULL,
 order_no VARCHAR(64), user_id VARCHAR(64), amount DECIMAL(19,2),
 channel VARCHAR(16), status VARCHAR(16), trade_no VARCHAR(128),
 created_at DATETIME(6), paid_at DATETIME(6), expire_at DATETIME(6), version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_payment_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE t_refund (
 refund_id VARCHAR(64) PRIMARY KEY, payment_id VARCHAR(64) NOT NULL, order_id VARCHAR(64),
 amount DECIMAL(19,2), reason VARCHAR(255), status VARCHAR(16), created_at DATETIME(6),
 UNIQUE KEY uk_refund_payment(payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
