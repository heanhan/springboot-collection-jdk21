package com.example.oauth.common.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * 自定义业务异常
 */
@Getter
@Setter
public class BizException extends RuntimeException {

    /** 错误码 */
    protected Integer errorCode;

    /** 错误信息 */
    protected String errorMsg;

    public BizException() {
        super();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface) {
        this.errorCode = errorInfoInterface.getResultCode();
        this.errorMsg = errorInfoInterface.getResultMsg();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface, Throwable cause) {
        this.errorCode = errorInfoInterface.getResultCode();
        this.errorMsg = errorInfoInterface.getResultMsg();
    }

    public BizException(String errorMsg) {
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public BizException(Integer errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public BizException(Integer errorCode, String errorMsg, Throwable cause) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
