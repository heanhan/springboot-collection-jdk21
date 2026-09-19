package com.example.ddd.auth.application.command;

/**
 * 命令：登录。
 *
 * @param username 登录名
 * @param password 明文密码
 * @param device   设备标识（可选）
 * @param ip       客户端 IP（由 Controller 从 HttpServletRequest 提取）
 */
public record LoginCommand(String username, String password, String device, String ip) {
}
