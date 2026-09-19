package com.example.ddd.common.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Money} 值对象单元测试。
 *
 * <p>验证：不可变性、币种校验、加减乘运算、四舍五入、负数拦截。</p>
 */
class MoneyTest {

    @Test
    @DisplayName("构造时会自动 scale 到 2 位小数并四舍五入")
    void shouldScaleToTwoDecimalPlaces() {
        Money m = Money.ofCny(new BigDecimal("10.005"));
        assertThat(m.amount()).isEqualByComparingTo("10.01"); // HALF_UP
        assertThat(m.currency()).isEqualTo(Money.CNY);
    }

    @Test
    @DisplayName("相同金额与币种的 Money 相等（值对象按值相等）")
    void shouldBeEqualByValue() {
        Money a = Money.ofCny("100.00");
        Money b = Money.ofCny(new BigDecimal("100"));
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("加法：不修改原对象，返回新对象")
    void addShouldReturnNewInstance() {
        Money a = Money.ofCny("10.00");
        Money b = Money.ofCny("20.50");
        Money sum = a.add(b);
        assertThat(sum.amount()).isEqualByComparingTo("30.50");
        assertThat(a.amount()).isEqualByComparingTo("10.00"); // 原对象未被修改
    }

    @Test
    @DisplayName("减法：结果为负时应抛异常")
    void subtractNegativeShouldThrow() {
        Money a = Money.ofCny("10.00");
        Money b = Money.ofCny("20.00");
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("减法结果为负");
    }

    @Test
    @DisplayName("乘法：单价 x 数量")
    void multiplyByQuantity() {
        Money unit = Money.ofCny("19.99");
        Money total = unit.multiply(3);
        assertThat(total.amount()).isEqualByComparingTo("59.97");
    }

    @Test
    @DisplayName("不同币种相加应拒绝")
    void differentCurrencyShouldThrow() {
        Money cny = Money.ofCny("10.00");
        Money usd = new Money(new BigDecimal("1.00"), java.util.Currency.getInstance("USD"));
        assertThatThrownBy(() -> cny.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("币种不一致");
    }

    @Test
    @DisplayName("负数金额应被拒绝")
    void negativeAmountShouldThrow() {
        assertThatThrownBy(() -> Money.ofCny("-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为负数");
    }

    @Test
    @DisplayName("比较大小")
    void greaterThan() {
        assertThat(Money.ofCny("10").greaterThan(Money.ofCny("5"))).isTrue();
        assertThat(Money.ofCny("5").greaterThanOrEqual(Money.ofCny("5"))).isTrue();
        assertThat(Money.zeroCny().isZero()).isTrue();
    }
}
