package com.example.ddd.auth.application.command;

/**
 * 命令：注册。
 *
 * @param username    登录名
 * @param rawPassword 明文密码
 * @param mobile      手机号
 * @param email       邮箱，可为 null
 * @param nickname    昵称（可选，透传给 user-service 建档）
 */
public record RegisterCommand(String username, String rawPassword, String mobile, String email, String nickname) {
}
