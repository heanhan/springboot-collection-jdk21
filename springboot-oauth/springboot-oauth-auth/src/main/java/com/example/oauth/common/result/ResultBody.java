package com.example.oauth.common.result;

import com.alibaba.fastjson2.JSONObject;
import com.example.oauth.common.enums.CommonEnum;
import com.example.oauth.common.exceptions.BaseErrorInfoInterface;

/**
 * 统一消息返回体
 * <p>
 * 与 springboot-common-starter 中的 ResultBody 保持一致，
 * 适配 Spring Boot 2.7.18 环境。
 *
 * @param <T> 数据类型
 */
public class ResultBody<T> {

    /** 响应代码 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应结果 */
    private T result;

    private BaseErrorInfoInterface errorInfo;

    public ResultBody() {
    }

    public ResultBody(BaseErrorInfoInterface errorInfo) {
        this.code = errorInfo.getResultCode();
        this.message = errorInfo.getResultMsg();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    // ==================== 静态工厂方法 ====================

    public static <T> ResultBody<T> success() {
        return success(CommonEnum.SUCCESS);
    }

    public static <T> ResultBody<T> success(T data) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(CommonEnum.SUCCESS.getResultCode());
        rb.setMessage(CommonEnum.SUCCESS.getResultMsg());
        rb.setResult(data);
        return rb;
    }

    public static <T> ResultBody<T> success(CommonEnum commonEnum) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(commonEnum.getResultCode());
        rb.setMessage(commonEnum.getResultMsg());
        return rb;
    }

    public static <T> ResultBody<T> error(BaseErrorInfoInterface errorInfo) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(errorInfo.getResultCode());
        rb.setMessage(errorInfo.getResultMsg());
        return rb;
    }

    public static <T> ResultBody<T> error(CommonEnum commonEnum) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(commonEnum.getResultCode());
        rb.setMessage(commonEnum.getResultMsg());
        return rb;
    }

    public static <T> ResultBody<T> error(Integer code, String message) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(code);
        rb.setMessage(message);
        return rb;
    }

    public static <T> ResultBody<T> error(String message) {
        ResultBody<T> rb = new ResultBody<>();
        rb.setCode(-1);
        rb.setMessage(message);
        return rb;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
