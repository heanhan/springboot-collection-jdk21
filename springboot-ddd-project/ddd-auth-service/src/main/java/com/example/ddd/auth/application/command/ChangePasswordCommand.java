package com.example.ddd.auth.application.command;

/**
 * 命令：修改密码。
 *
 * @param userId      用户 ID
 * @param oldPassword 原密码
 * @param newPassword 新密码
 */
public record ChangePasswordCommand(String userId, String oldPassword, String newPassword) {
}
