-- 同业务号操作序列化及取消屏障，库存热点行仍使用乐观锁。
CREATE TABLE t_stock_operation (
 biz_no VARCHAR(128) PRIMARY KEY, state VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO t_stock_operation(biz_no,state)
SELECT biz_no,CASE WHEN MAX(type='DEDUCT')=1 THEN 'DEDUCTED'
                  WHEN MAX(type='UNLOCK')=1 THEN 'RELEASED' ELSE 'LOCKED' END
FROM t_stock_transaction WHERE type IN ('LOCK','DEDUCT','UNLOCK') GROUP BY biz_no;
