package com.example.ddd.common;

import com.example.ddd.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** 共享内核测试：金额运算与非负不变式。 */
class MoneyTest {
    @Test void calculatesExactly() {
        assertThat(Money.ofCny("0.10").add(Money.ofCny("0.20")).amount()).isEqualByComparingTo("0.30");
        assertThat(Money.ofCny("9.99").multiply(3).amount()).isEqualByComparingTo("29.97");
    }
    @Test void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.ofCny("-1")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> Money.ofCny("1").subtract(Money.ofCny("2"))).isInstanceOf(RuntimeException.class);
    }
}
