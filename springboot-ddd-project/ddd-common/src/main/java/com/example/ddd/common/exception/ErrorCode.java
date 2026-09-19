package com.example.ddd.common.exception;

/**
 * 全局业务错误码枚举。
 *
 * <p><b>编码规则：</b>
 * 采用"服务前缀 + 4 位数字"的方式，避免各服务错误码冲突：
 * <ul>
 *   <li>{@code 0000}      成功</li>
 *   <li>{@code 0xxx}      通用错误 (参数、系统、鉴权通用)</li>
 *   <li>{@code 1xxx}      auth-service     认证鉴权</li>
 *   <li>{@code 2xxx}      user-service     用户中心</li>
 *   <li>{@code 3xxx}      product-service  商品目录</li>
 *   <li>{@code 4xxx}      inventory-service 库存</li>
 *   <li>{@code 5xxx}      order-service    订单</li>
 *   <li>{@code 6xxx}      payment-service  支付</li>
 *   <li>{@code 7xxx}      logistics-service 物流</li>
 *   <li>{@code 8xxx}      cart-service     购物车</li>
 * </ul>
 *
 * <p><b>为什么用枚举？</b>
 * 集中管理，IDE 可跳转，避免"魔法字符串"散落在代码里；
 * 每个错误码都有明确的中文消息，方便前端直接展示或国际化替换。</p>
 *
 * @author ddd-learning
 */
public enum ErrorCode {

    // ========== 通用 0xxx ==========
    SUCCESS("0000", "操作成功"),
    BAD_REQUEST("0001", "请求参数不合法"),
    UNAUTHORIZED("0002", "未认证或认证已过期"),
    FORBIDDEN("0003", "无访问权限"),
    NOT_FOUND("0004", "资源不存在"),
    METHOD_NOT_ALLOWED("0005", "请求方法不被允许"),
    CONFLICT("0006", "资源状态冲突"),
    SYSTEM_ERROR("0999", "系统繁忙，请稍后再试"),
    REMOTE_CALL_ERROR("0998", "远程服务调用失败"),

    // ========== 认证鉴权 1xxx ==========
    AUTH_USERNAME_EXISTS("1001", "用户名已存在"),
    AUTH_USER_NOT_FOUND("1002", "用户不存在"),
    AUTH_PASSWORD_INCORRECT("1003", "密码错误"),
    AUTH_ACCOUNT_LOCKED("1004", "账号已锁定，请稍后再试"),
    AUTH_ACCOUNT_DISABLED("1005", "账号已停用"),
    AUTH_TOKEN_INVALID("1006", "Token 无效或已过期"),
    AUTH_TOKEN_EXPIRED("1007", "Token 已过期，请重新登录"),
    AUTH_REFRESH_TOKEN_INVALID("1008", "RefreshToken 无效"),
    AUTH_LOGIN_TOO_FREQUENT("1009", "登录失败次数过多，账号临时锁定"),
    AUTH_OLD_PASSWORD_INCORRECT("1010", "原密码不正确"),

    // ========== 用户中心 2xxx ==========
    USER_NOT_FOUND("2001", "用户不存在"),
    USER_ALREADY_EXISTS("2002", "用户已存在"),
    USER_MOBILE_DUPLICATE("2003", "手机号已被使用"),
    USER_ADDRESS_NOT_FOUND("2004", "收货地址不存在"),
    USER_ADDRESS_LIMIT_EXCEEDED("2005", "收货地址数量已达上限"),
    USER_ROLE_NOT_FOUND("2006", "角色不存在"),
    USER_PERMISSION_DENIED("2007", "权限不足"),

    // ========== 商品 3xxx ==========
    PRODUCT_SPU_NOT_FOUND("3001", "商品不存在"),
    PRODUCT_SKU_NOT_FOUND("3002", "SKU 不存在"),
    PRODUCT_OFF_SHELF("3003", "商品已下架"),
    PRODUCT_CATEGORY_NOT_FOUND("3004", "商品类目不存在"),
    PRODUCT_ALREADY_ON_SALE("3005", "商品已上架"),
    PRODUCT_ALREADY_OFF_SHELF("3006", "商品已下架"),
    PRODUCT_STATUS_ILLEGAL("3007", "商品状态不允许此操作"),
    PRODUCT_BRAND_NOT_FOUND("3008", "品牌不存在"),

    // ========== 库存 4xxx ==========
    INVENTORY_WAREHOUSE_NOT_FOUND("4001", "仓库不存在"),
    INVENTORY_STOCK_NOT_FOUND("4002", "库存记录不存在"),
    INVENTORY_STOCK_SHORTAGE("4003", "库存不足"),
    INVENTORY_LOCK_FAILED("4004", "库存预占失败，请重试"),
    INVENTORY_TRANSACTION_DUPLICATE("4005", "库存流水重复"),

    // ========== 订单 5xxx ==========
    ORDER_NOT_FOUND("5001", "订单不存在"),
    ORDER_STATUS_ILLEGAL("5002", "订单状态非法，无法执行该操作"),
    ORDER_ALREADY_PAID("5003", "订单已支付"),
    ORDER_ALREADY_CANCELLED("5004", "订单已取消"),
    ORDER_ITEM_EMPTY("5005", "订单商品为空"),
    ORDER_AMOUNT_MISMATCH("5006", "订单金额不一致"),
    ORDER_TIMEOUT_CANCELLED("5007", "订单超时未支付已自动取消"),

    // ========== 支付 6xxx ==========
    PAYMENT_ORDER_NOT_FOUND("6001", "支付单不存在"),
    PAYMENT_ORDER_ALREADY_PAID("6002", "支付单已支付"),
    PAYMENT_ORDER_CLOSED("6003", "支付单已关闭"),
    PAYMENT_CHANNEL_UNSUPPORTED("6004", "不支持的支付渠道"),
    PAYMENT_AMOUNT_MISMATCH("6005", "支付金额不一致"),
    PAYMENT_REFUND_EXCEED("6006", "退款金额超出可退金额"),
    PAYMENT_CALLBACK_VERIFY_FAILED("6007", "支付回调验签失败"),

    // ========== 物流 7xxx ==========
    LOGISTICS_SHIPMENT_NOT_FOUND("7001", "发货单不存在"),
    LOGISTICS_SHIPMENT_ALREADY_SHIPPED("7002", "已发货，不能重复发货"),
    LOGISTICS_SHIPMENT_ALREADY_DELIVERED("7003", "已签收"),
    LOGISTICS_CARRIER_UNSUPPORTED("7004", "不支持的承运商"),

    // ========== 购物车 8xxx ==========
    CART_EMPTY("8001", "购物车为空"),
    CART_ITEM_NOT_FOUND("8002", "购物车项不存在"),
    CART_ITEM_LIMIT_EXCEEDED("8003", "购物车条目数已达上限");

    /** 错误码字符串，例如 "5002" */
    private final String code;

    /** 默认错误消息（可被 BusinessException 覆盖） */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
