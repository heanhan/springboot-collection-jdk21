package com.example.ddd.contract.user;

import java.io.Serializable;

/**
 * 用户基本信息 DTO（跨上下文使用）。
 *
 * <p><b>为什么不直接暴露 User 聚合根？</b>
 * 领域模型是<b>上下文内部</b>的资产，外部上下文只需要"最小必要信息"。
 * DTO 是发布语言的一部分，变更需要谨慎；聚合根可以根据业务需要自由重构。</p>
 *
 * @param userId      用户 ID
 * @param nickname    昵称
 * @param avatar      头像 URL
 * @param mobile      手机号（脱敏后）
 * @param memberLevel 会员等级码，例如 NORMAL / SILVER / GOLD / DIAMOND
 * @param status      状态：ACTIVE / DISABLED
 */
public record UserDTO(String userId,
                      String nickname,
                      String avatar,
                      String mobile,
                      String memberLevel,
                      String status) implements Serializable {
}
