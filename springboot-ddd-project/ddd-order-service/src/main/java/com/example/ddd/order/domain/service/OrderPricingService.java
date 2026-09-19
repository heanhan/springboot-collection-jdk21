package com.example.ddd.order.domain.service;

import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.order.domain.model.entity.OrderItem;

import java.util.List;

/**
 * 领域服务：订单定价 (Order Pricing)。
 *
 * <p><b>为什么是领域服务而不是聚合根方法？</b>
 * 定价规则（运费、优惠）会随业务演进变得复杂，未来需要调用促销 / 优惠券 / 会员折扣等外部信息。
 * 把这些"跨概念的计算"放进领域服务，聚合根只负责守护最终不变式（pay == total + shipping - discount），
 * 职责更清晰。当前为学习版：运费采用"满 99 元包邮，否则 10 元"的简单规则，优惠恒为 0。</p>
 *
 * <p><b>防腐层预留：</b>真实项目中促销计算应通过 ACL 调用 promotion 上下文，
 * 本类是接入点——将来把 discount 的计算替换为远程调用即可，聚合根无感知。</p>
 *
 * <p><b>无 Spring 依赖：</b>由 infrastructure 层的 {@code DomainServiceConfig} 显式装配。</p>
 */
public class OrderPricingService {

    /** 包邮门槛（元） */
    private static final Money FREE_SHIPPING_THRESHOLD = Money.ofCny("99.00");
    /** 基础运费（元） */
    private static final Money BASE_SHIPPING_FEE = Money.ofCny("10.00");

    /**
     * 根据订单项计算金额。
     *
     * @param items 订单项列表（非空）
     * @return 定价结果（总额 / 运费 / 优惠 / 实付）
     */
    public PricingResult price(List<OrderItem> items) {
        Money total = Money.zeroCny();
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        Money shippingFee = total.greaterThanOrEqual(FREE_SHIPPING_THRESHOLD) ? Money.zeroCny() : BASE_SHIPPING_FEE;
        Money discount = Money.zeroCny();
        Money pay = total.add(shippingFee).subtract(discount);
        return new PricingResult(total, shippingFee, discount, pay);
    }

    /**
     * 值对象：定价结果。
     *
     * @param totalAmount    商品总额
     * @param shippingFee    运费
     * @param discountAmount 优惠金额
     * @param payAmount      实付金额 = total + shipping - discount
     */
    public record PricingResult(Money totalAmount, Money shippingFee, Money discountAmount, Money payAmount) {
    }
}
