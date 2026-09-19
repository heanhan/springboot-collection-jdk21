package com.example.ddd.inventory.infrastructure.persistence.repository;

import com.example.ddd.inventory.application.port.StockOperationGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 基础设施业务锁：只串行化同订单，不同订单的热点库存仍通过 @Version 竞争与重试。 */
@Component
public class JdbcStockOperationGuard implements StockOperationGuard {
    private final JdbcTemplate jdbc;
    public JdbcStockOperationGuard(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public String lock(String bizNo) {
        jdbc.update("INSERT INTO t_stock_operation(biz_no,state) VALUES (?,'NEW') ON DUPLICATE KEY UPDATE biz_no=biz_no",bizNo);
        return jdbc.queryForObject("SELECT state FROM t_stock_operation WHERE biz_no=? FOR UPDATE",String.class,bizNo);
    }
    public void state(String bizNo,String state) { jdbc.update("UPDATE t_stock_operation SET state=? WHERE biz_no=?",state,bizNo); }
}
