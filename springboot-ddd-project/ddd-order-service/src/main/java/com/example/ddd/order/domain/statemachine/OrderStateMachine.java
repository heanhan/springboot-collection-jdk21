package com.example.ddd.order.domain.statemachine;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.order.domain.model.valueobject.OrderStatus;

/**
 * 领域概念：订单状态机 (Order State Machine)。
 *
 * <p><b>为什么单独抽一个状态机？</b>
 * 订单状态的合法流转是核心业务规则。若散落在 {@code Order.pay()}、{@code Order.cancel()} 各处用 if 判断，
 * 规则会重复且容易遗漏。集中到状态机后，"哪些状态能到哪些状态"一目了然，且易于扩展（如加退款流程）。</p>
 *
 * <p><b>JDK 21 特性：</b>用 {@code switch} 表达式 + 箭头语法穷举每个源状态，
 * 编译器会检查枚举是否覆盖完全（配合 default 兜底），比 if-else 链更安全。</p>
 *
 * <p><b>无状态：</b>本类不持有任何字段，是纯函数式的领域规则集合，因此用静态方法。</p>
 */
public final class OrderStateMachine {

    private OrderStateMachine() {}

    /**
     * 判断从 {@code from} 到 {@code to} 是否为合法转换。
     */
    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case CREATED -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.REFUNDING;
            case SHIPPED -> to == OrderStatus.COMPLETED || to == OrderStatus.REFUNDING;
            case REFUNDING -> to == OrderStatus.REFUNDED;
            case COMPLETED -> to == OrderStatus.REFUNDING;
            // 终态不可再流转
            case CANCELLED, REFUNDED -> false;
        };
    }

    /**
     * 断言转换合法，否则抛业务异常。
     *
     * @throws BusinessException {@link ErrorCode#ORDER_STATUS_ILLEGAL}
     */
    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ILLEGAL,
                    String.format("非法的订单状态流转: %s -> %s", from, to));
        }
    }
}
