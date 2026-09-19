package com.example.ddd.inventory;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.inventory.application.command.LockStockCommand;
import com.example.ddd.inventory.application.port.DomainEventPublisher;
import com.example.ddd.inventory.application.service.StockApplicationService;
import com.example.ddd.inventory.domain.repository.*;
import com.example.ddd.inventory.domain.service.StockReservationService;
import com.example.ddd.inventory.infrastructure.persistence.repository.JdbcStockOperationGuard;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 事务测试：取消先到不得悬挂预占；实扣后的释放不得误释放其他订单库存。 */
class StockOperationGuardTest {
    @Test void cancellationBarrierAndDeductedRelease() {
        var ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:stock_guard;MODE=MySQL;DB_CLOSE_DELAY=-1");
        var jdbc=new JdbcTemplate(ds); jdbc.execute("CREATE TABLE t_stock_operation(biz_no VARCHAR PRIMARY KEY,state VARCHAR)");
        var stocks=mock(StockRepository.class); var transactions=mock(StockTransactionRepository.class);
        var guard=new JdbcStockOperationGuard(jdbc); var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var service=new StockApplicationService(stocks,transactions,mock(StockReservationService.class),mock(DomainEventPublisher.class),tx,3,guard);
        service.release("cancel-first","取消");
        assertThat(jdbc.queryForObject("SELECT state FROM t_stock_operation WHERE biz_no='cancel-first'",String.class)).isEqualTo("RELEASED");
        assertThatThrownBy(() -> service.lock(new LockStockCommand("cancel-first",List.of(new LockStockCommand.Item("sku",null,1)))))
                .isInstanceOf(BusinessException.class);
        jdbc.update("INSERT INTO t_stock_operation VALUES ('paid','DEDUCTED')");
        service.release("paid","退款");
        assertThat(jdbc.queryForObject("SELECT state FROM t_stock_operation WHERE biz_no='paid'",String.class)).isEqualTo("DEDUCTED");
        verifyNoInteractions(stocks);
    }
}
