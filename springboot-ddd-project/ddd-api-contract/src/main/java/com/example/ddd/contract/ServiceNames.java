package com.example.ddd.contract;

/**
 * 各服务在 Feign / 服务发现中使用的名称常量。
 *
 * <p>集中定义避免字符串散落在代码里，改动一处即可影响全局。</p>
 *
 * @author ddd-learning
 */
public final class ServiceNames {

    /** 认证鉴权服务 */
    public static final String AUTH = "ddd-auth-service";

    /** 用户中心服务 */
    public static final String USER = "ddd-user-service";

    /** 商品服务 */
    public static final String PRODUCT = "ddd-product-service";

    /** 库存服务 */
    public static final String INVENTORY = "ddd-inventory-service";

    /** 订单服务 */
    public static final String ORDER = "ddd-order-service";

    /** 支付服务 */
    public static final String PAYMENT = "ddd-payment-service";

    /** 物流服务 */
    public static final String LOGISTICS = "ddd-logistics-service";

    /** 购物车服务 */
    public static final String CART = "ddd-cart-service";

    private ServiceNames() {
    }
}
