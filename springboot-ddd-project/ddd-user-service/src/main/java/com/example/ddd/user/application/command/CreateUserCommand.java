package com.example.ddd.user.application.command;

/**
 * 命令对象 (Command)：创建用户。
 *
 * <p><b>为什么用 record？</b>
 * Command 是不可变的输入 DTO，record 天然不可变、自带 equals/hashCode/toString，
 * 是 JDK 14+ 表达 Command 的最佳选择。</p>
 *
 * <p><b>Command vs Entity：</b>
 * Command 是"意图"（想做什么），Entity 是"状态"（当前是什么）。
 * 应用服务接收 Command，转换为对领域对象的方法调用。</p>
 *
 * @param nickname 昵称
 * @param mobile   手机号（明文，值对象在领域层构造时校验）
 * @param email    邮箱，可为 null
 * @param gender   性别码 0/1/2，可为 null
 */
public record CreateUserCommand(String nickname, String mobile, String email, Integer gender) {
}
