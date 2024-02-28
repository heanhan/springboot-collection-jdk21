package com.example.dynamic.jpa.system.config;


import com.example.dynamic.jpa.system.vo.LoginInfo;

/**
 * @Author: zhaojh
 * @ClassName: LoginInfoHolder
 * @Description: 存放用户登录标识信息，利用ThreadLocal封装的保存数据源上线的上下文
 */
public class LoginInfoHolder {

    private static final ThreadLocal<LoginInfo> CONTEXT = new ThreadLocal<>();

    public static void setTenant(LoginInfo loginInfo) {
        CONTEXT.set(loginInfo);
    }

    public static LoginInfo getTenant() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
