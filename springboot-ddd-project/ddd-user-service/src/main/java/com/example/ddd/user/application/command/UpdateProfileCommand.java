package com.example.ddd.user.application.command;

/**
 * 命令：修改用户资料。
 *
 * @param userId  用户 ID
 * @param nickname 新昵称，null 表示不改
 * @param avatar   新头像，null 表示不改
 * @param gender   新性别码，null 表示不改
 * @param email    新邮箱，null 表示不改
 */
public record UpdateProfileCommand(String userId, String nickname, String avatar, Integer gender, String email) {
}
