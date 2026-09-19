package com.example.ddd.user.domain.model.valueobject;

/**
 * 值对象：会员等级 (MemberLevel)。
 *
 * <p><b>为什么用枚举而不是实体？</b>
 * 会员等级是一个有限的、离散的、无独立标识的分类概念，天然适合枚举值对象。
 * 每个等级携带"折扣率"这个业务属性，用于订单计价时应用会员优惠。</p>
 *
 * <p><b>升级规则：</b>
 * 由 domain.service.MemberLevelPromotionService 根据用户历史订单金额计算，
 * 而不是在这里硬编码，保持值对象纯粹。</p>
 *
 * @author ddd-learning
 */
public enum MemberLevel {

    /** 普通会员 (注册默认)：无折扣 */
    NORMAL("普通会员", 1.00),

    /** 银牌会员：累计消费 ≥ 1000 元，95 折 */
    SILVER("银牌会员", 0.95),

    /** 金牌会员：累计消费 ≥ 5000 元，9 折 */
    GOLD("金牌会员", 0.90),

    /** 钻石会员：累计消费 ≥ 20000 元，85 折 */
    DIAMOND("钻石会员", 0.85);

    private final String displayName;
    private final double discountRate;

    MemberLevel(String displayName, double discountRate) {
        this.displayName = displayName;
        this.discountRate = discountRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 折扣率：0.85 表示 85 折 */
    public double getDiscountRate() {
        return discountRate;
    }

    /**
     * 判断当前等级是否高于另一个等级（枚举声明顺序即优先级）。
     */
    public boolean isHigherThan(MemberLevel other) {
        return this.ordinal() > other.ordinal();
    }
}
