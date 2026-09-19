package com.example.ddd.common.result;

import com.example.ddd.common.exception.ErrorCode;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一 HTTP 响应封装。
 *
 * <p><b>设计原则：</b>
 * <ul>
 *   <li>成功与失败共用一个包装类，前端只需判断 {@code code == "0000"} 即可。</li>
 *   <li>携带 traceId 便于日志追踪。</li>
 *   <li>使用泛型 {@code T} 表达 data 类型，配合 springdoc 生成 OpenAPI 文档。</li>
 * </ul>
 *
 * <p><b>为什么不用 record？</b>
 * record 无法提供泛型的静态工厂方法链式调用（{@code Result.<T>ok()} 语法别扭），
 * 且我们需要保留无参构造给 Jackson 反序列化使用。</p>
 *
 * @param <T> data 字段的类型
 * @author ddd-learning
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final String SUCCESS_CODE = "0000";
    /** 成功消息 */
    public static final String SUCCESS_MESSAGE = "success";

    /** 业务状态码，"0000" 表示成功，其他表示失败 */
    private String code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 请求追踪 ID，用于关联日志 */
    private String traceId;

    /** 服务端时间戳 (毫秒) */
    private long timestamp;

    public Result() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    private Result(String code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ============================================================
    // 静态工厂方法
    // ============================================================

    /** 成功，无数据 */
    public static <T> Result<T> ok() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /** 成功，带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /** 成功，带数据和自定义消息 */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    /** 失败，使用错误码枚举 */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 失败，使用错误码枚举 + 自定义消息（覆盖默认消息） */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /** 失败，直接指定 code 和 message */
    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    // ============================================================
    // Getters / Setters (Jackson 反序列化需要)
    // ============================================================

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** RPC 防腐层取值：业务失败或缺失数据时快速失败，不能把空值当成功。 */
    public T requireData() {
        if (!isSuccess() || data == null) {
            throw new com.example.ddd.common.exception.BusinessException(ErrorCode.BAD_REQUEST,
                    message == null ? "远程服务未返回有效数据" : message);
        }
        return data;
    }

    /** 判断当前 Result 是否为成功状态 */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }
}
