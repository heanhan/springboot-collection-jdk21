package com.example.ddd.user.application.command;

/**
 * 命令：为用户分配 / 移除角色。
 *
 * @param userId 用户 ID
 * @param roleId 角色 ID
 * @param assign true = 分配，false = 移除
 */
public record AssignRoleCommand(String userId, String roleId, boolean assign) {
}
