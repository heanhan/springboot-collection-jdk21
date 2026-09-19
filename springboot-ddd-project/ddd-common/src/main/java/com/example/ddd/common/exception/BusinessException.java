package com.example.ddd.common.exception;

/**
 * 业务异常。
 *
 * <p><b>为什么要自定义业务异常？</b>
 * <ul>
 *   <li>把"可预期的业务错误"（例：库存不足）与"不可预期的系统错误"（例：NPE）区分开。</li>
 *   <li>携带 {@link ErrorCode}，全局异常处理器可以直接映射为统一响应。</li>
 *   <li>使用 {@code RuntimeException}，无需在方法签名上到处 throws，符合业务代码习惯。</li>
 * </ul>
 *
 * <p><b>使用建议：</b>
 * <ul>
 *   <li>领域层校验失败 → 直接抛 BusinessException，不要返回 null 或特殊值。</li>
 *   <li>应用服务捕获后不要吞掉，交给全局异常处理器统一转换。</li>
 *   <li>不要在循环中抛异常做流程控制。</li>
 * </ul>
 *
 * @author ddd-learning
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码枚举，非空 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode.getCode();
    }

    // ============================================================
    // 常用静态工厂：让业务代码更简洁
    // ============================================================

    /** 断言条件为真，否则抛异常 */
    public static void assertTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BusinessException(errorCode);
        }
    }

    /** 断言条件为真，否则抛自定义消息 */
    public static void assertTrue(boolean expression, ErrorCode errorCode, String message) {
        if (!expression) {
            throw new BusinessException(errorCode, message);
        }
    }

    /** 断言对象非空 */
    public static <T> T assertNotNull(T obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
        return obj;
    }

    /** 断言对象非空，自定义消息 */
    public static <T> T assertNotNull(T obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new BusinessException(errorCode, message);
        }
        return obj;
    }
}
