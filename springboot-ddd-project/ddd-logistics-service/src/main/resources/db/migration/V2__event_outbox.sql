-- 业务事务与待发送事件原子提交，后台轮询已提交事件。
CREATE TABLE t_event_outbox (
 event_id VARCHAR(64) PRIMARY KEY, destination VARCHAR(160) NOT NULL, payload LONGTEXT NOT NULL,
 sent TINYINT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_outbox_pending(sent,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
