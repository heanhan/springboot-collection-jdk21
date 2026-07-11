package com.example.oauth.common.enums;

import com.example.oauth.common.exceptions.BaseErrorInfoInterface;

/**
 * 认证授权相关错误码枚举
 */
public enum AuthErrorEnum implements BaseErrorInfoInterface {

    /** Token 无效 */
    TOKEN_INVALID(4011, "Token 无效或已过期"),
    /** Token 已被加入黑名单 */
    TOKEN_BLACKLISTED(4012, "Token 已失效，请重新登录"),
    /** Refresh Token 无效 */
    REFRESH_TOKEN_INVALID(4013, "Refresh Token 无效或已过期"),
    /** 用户已被锁定 */
    USER_LOCKED(4014, "账号已被锁定，请 30 分钟后重试"),
    /** 登录失败次数超限 */
    LOGIN_FAIL_LIMIT(4015, "登录失败次数过多，账号已被锁定 30 分钟"),
    /** 用户已禁用 */
    USER_DISABLED(4016, "账号已被禁用"),
    /** 未登录 */
    NOT_LOGGED_IN(4017, "未登录或登录已过期，请重新登录"),
    /** 无权限访问 */
    ACCESS_DENIED(4031, "无权限访问该资源"),
    /** 客户端认证失败 */
    CLIENT_AUTH_FAILED(4018, "客户端认证失败"),
    /** 不支持的授权类型 */
    UNSUPPORTED_GRANT_TYPE(4019, "不支持的授权类型"),
    /** 验证码错误 */
    CAPTCHA_INVALID(4020, "验证码错误"),
    /** 验证码已过期 */
    CAPTCHA_EXPIRED(4021, "验证码已失效，请重新获取");

    private final Integer resultCode;
    private final String resultMsg;

    AuthErrorEnum(Integer resultCode, String resultMsg) {
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
