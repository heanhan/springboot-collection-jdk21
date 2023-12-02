package org.example.commons.exceptins;

/**
 * @Classname BaseErrorInfoInterface
 * @Description 基础的错误信息接口
 * @Date 2023/4/28 10:56 上午
 * @Created by zhaojh0912
 */
public interface BaseErrorInfoInterface {


    /**
     * 错误码
     */
    Integer getResultCode();

    /**
     * 错误描述
     */
    String getResultMsg();
}
