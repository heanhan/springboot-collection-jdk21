package com.example.oauth.common.exceptions;

/**
 * 基础错误信息接口
 */
public interface BaseErrorInfoInterface {

    /** 错误码 */
    Integer getResultCode();

    /** 错误描述 */
    String getResultMsg();
}
