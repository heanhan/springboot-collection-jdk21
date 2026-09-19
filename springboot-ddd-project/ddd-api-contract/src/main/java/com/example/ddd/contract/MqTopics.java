package com.example.ddd.contract;

/**
 * RocketMQ 主题 (Topic) 与标签 (Tag) 常量。
 *
 * <p><b>命名规范：</b>
 * Topic 使用 {@code ddd-<context>-event}，Tag 使用具体事件类名。
 * 消费方通过 {@code topic + tag} 精确订阅需要的事件。</p>
 *
 * @author ddd-learning
 */
public final class MqTopics {

    /** 订单事件 Topic */
    public static final String ORDER_EVENT = "ddd-order-event";

    /** 支付事件 Topic */
    public static final String PAYMENT_EVENT = "ddd-payment-event";

    /** 物流事件 Topic */
    public static final String LOGISTICS_EVENT = "ddd-logistics-event";

    /** 库存事件 Topic */
    public static final String INVENTORY_EVENT = "ddd-inventory-event";

    /** 用户事件 Topic */
    public static final String USER_EVENT = "ddd-user-event";

    /** 认证事件 Topic */
    public static final String AUTH_EVENT = "ddd-auth-event";

    /** 商品事件 Topic */
    public static final String PRODUCT_EVENT = "ddd-product-event";

    // ============================================================
    // Tag = 事件类型
    // ============================================================

    public static final String TAG_ORDER_CREATED = "OrderCreated";
    public static final String TAG_ORDER_PAID = "OrderPaid";
    public static final String TAG_ORDER_SHIPPED = "OrderShipped";
    public static final String TAG_ORDER_COMPLETED = "OrderCompleted";
    public static final String TAG_ORDER_CANCELLED = "OrderCancelled";

    public static final String TAG_PAYMENT_SUCCESS = "PaymentSuccess";
    public static final String TAG_PAYMENT_FAILED = "PaymentFailed";
    public static final String TAG_REFUND_SUCCESS = "RefundSuccess";

    public static final String TAG_SHIPMENT_CREATED = "ShipmentCreated";
    public static final String TAG_SHIPMENT_DELIVERED = "ShipmentDelivered";

    public static final String TAG_STOCK_LOCKED = "StockLocked";
    public static final String TAG_STOCK_DEDUCTED = "StockDeducted";
    public static final String TAG_STOCK_RELEASED = "StockReleased";

    public static final String TAG_USER_REGISTERED = "UserRegistered";
    public static final String TAG_USER_DISABLED = "UserDisabled";

    public static final String TAG_ACCOUNT_CREATED = "AccountCreated";
    public static final String TAG_ACCOUNT_PASSWORD_CHANGED = "AccountPasswordChanged";

    public static final String TAG_PRODUCT_PUBLISHED = "ProductPublished";
    public static final String TAG_PRODUCT_OFF_SHELF = "ProductOffShelf";
    public static final String TAG_SKU_PRICE_CHANGED = "SkuPriceChanged";

    private MqTopics() {
    }
}
