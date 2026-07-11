package com.example.oauth.common.enums;


import com.example.oauth.common.exceptions.BaseErrorInfoInterface;

/**
 * 通用错误码枚举
 * <p>
 * 与 springboot-common-starter 中的 CommonEnum 保持一致。
 */
public enum CommonEnum implements BaseErrorInfoInterface {

    SUCCESS(200, "操作成功"),
    ERROR(500, "系统错误"),
    ERROR_INSERT(501, "系统错误:新增操作错误！"),
    ERROR_DELETED(502, "系统错误:删除操作错误！"),
    ERROR_UPDATE(500, "系统错误:更新操作错误！"),
    ERROR_SELECT(500, "系统错误:查询操作错误！"),
    FAILED(101, "操作失败"),
    UNAUTHORIZED(102, "登录超时"),
    PARAM_ERROR(103, "参数错误"),
    INVALID_PARAM_EXIST(104, "请求参数已存在"),
    INVALID_PARAM_EMPTY(105, "请求参数为空"),
    PARAM_TYPE_MISMATCH(106, "参数类型不匹配"),
    PARAM_VALID_ERROR(107, "参数校验失败"),
    ILLEGAL_REQUEST(108, "非法请求"),
    ARITHMETIC_EXCEPTION(109, "算数异常"),
    INVALID_VCODE(204, "验证码错误"),
    INVALID_USERNAME_PASSWORD(205, "账号或密码错误"),
    INVALID_RE_PASSWORD(206, "两次输入密码不一致"),
    INVALID_OLD_PASSWORD(207, "旧密码错误"),
    USERNAME_ALREADY_IN(208, "用户名已存在"),
    INVALID_USERNAME(209, "用户名不存在"),
    INVALID_ROLE(210, "角色不存在"),
    ROLE_USED(211, "角色使用中，不可删除"),
    NO_PERMISSION(403, "当前用户无该接口权限"),
    CUSTOM_EXCEPTION(1001, "用户自定义的异常");

    /** 错误码 */
    private final Integer resultCode;

    /** 错误描述 */
    private final String resultMsg;

    CommonEnum(Integer resultCode, String resultMsg) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }

    @Override
    public Integer getResultCode() {
        return resultCode;
    }

    @Override
    public String getResultMsg() {
        return resultMsg;
    }
}
