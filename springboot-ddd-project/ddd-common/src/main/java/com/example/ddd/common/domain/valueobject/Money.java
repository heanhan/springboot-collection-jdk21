package com.example.ddd.common.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 值对象 (Value Object)：金额 Money。
 *
 * <p><b>为什么是值对象而不是实体？</b>
 * 金额没有独立身份标识 —— 两张 100 元人民币在业务上完全等价，可以互换。
 * 值对象的三个特征：
 * <ol>
 *   <li><b>无标识</b>：不通过 ID 区分，通过属性值区分。</li>
 *   <li><b>不可变</b>：一旦创建不能修改，任何"修改"都是产生一个新对象。</li>
 *   <li><b>按值相等</b>：{@code equals} 比较所有属性。</li>
 * </ol>
 *
 * <p><b>为什么不用 double？</b>
 * double 是二进制浮点数，无法精确表示十进制小数（例如 0.1 + 0.2 != 0.3）。
 * 金融场景必须用 {@link BigDecimal}，且明确指定 scale (小数位) 与 RoundingMode。</p>
 *
 * <p><b>为什么带 Currency？</b>
 * 只保留 BigDecimal 会丢失币种信息，跨币种相加会得到荒谬的结果。
 * 本类强制在运算前校验币种一致，避免"1 美元 + 1 人民币 = 2"这种错误。</p>
 *
 * <p><b>使用 record 的原因：</b>
 * JDK 21 的 record 天然满足"不可变 + 按值相等 + 简洁"，是值对象的最佳载体。</p>
 *
 * @param amount   金额，非 null，scale 必须为 2 (人民币分)
 * @param currency 币种，非 null
 * @author ddd-learning
 */
public record Money(BigDecimal amount, Currency currency) {

    /** 人民币常量，业务默认币种 */
    public static final Currency CNY = Currency.getInstance("CNY");

    /** 零金额 (CNY) */
    public static final Money ZERO_CNY = new Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), CNY);

    /**
     * 紧凑构造器 (Compact Constructor)：所有创建路径都会经过这里，做统一校验和规范化。
     */
    public Money {
        Objects.requireNonNull(amount, "Money.amount 不能为 null");
        Objects.requireNonNull(currency, "Money.currency 不能为 null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money.amount 不能为负数：" + amount);
        }
        // 统一保留 2 位小数，四舍五入
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    /** 便捷工厂：CNY 金额 */
    public static Money ofCny(String amount) {
        return new Money(new BigDecimal(amount), CNY);
    }

    /** 便捷工厂：CNY 金额 */
    public static Money ofCny(BigDecimal amount) {
        return new Money(amount, CNY);
    }

    /** 便捷工厂：CNY 零金额 */
    public static Money zeroCny() {
        return ZERO_CNY;
    }

    /**
     * 加法：返回新对象，币种必须一致。
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 减法：结果不能为负。
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("Money 减法结果为负：" + this + " - " + other);
        }
        return new Money(result, this.currency);
    }

    /**
     * 乘以数量：例如"单价 x 数量 = 小计"。
     *
     * @param multiplier 乘数 (整数或小数)，不能为负
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "乘数不能为 null");
        if (multiplier.signum() < 0) {
            throw new IllegalArgumentException("乘数不能为负：" + multiplier);
        }
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /** 乘以整数数量 */
    public Money multiply(int quantity) {
        return multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 是否大于另一个金额。
     */
    public boolean greaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /** 是否大于等于 */
    public boolean greaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /** 是否为零 */
    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    /** 是否为正 */
    public boolean isPositive() {
        return this.amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "另一个 Money 不能为 null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("币种不一致，无法运算：" + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return currency.getSymbol() + " " + amount.toPlainString();
    }
}
