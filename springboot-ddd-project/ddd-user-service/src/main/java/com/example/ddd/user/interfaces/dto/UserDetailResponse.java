package com.example.ddd.user.interfaces.dto;

import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 响应 DTO：用户详情（面向前端）。
 *
 * <p><b>为什么不直接返回 User 聚合根？</b>
 * <ul>
 *   <li>聚合根包含内部字段（如 domainEvents），序列化会污染 JSON。</li>
 *   <li>UI 需要的字段与领域模型未必一一对应（例如脱敏、拼接）。</li>
 *   <li>DTO 是稳定的对外契约，Domain 可自由重构。</li>
 * </ul>
 */
public record UserDetailResponse(
        String userId,
        String nickname,
        String avatar,
        String mobile,
        String email,
        Integer gender,
        String memberLevel,
        String status,
        LocalDateTime registerTime,
        List<String> roleIds,
        List<AddressItem> addresses
) {

    /**
     * 地址条目 DTO（内嵌）。
     */
    public record AddressItem(
            String addressId,
            String receiver,
            String mobile,
            String province,
            String city,
            String district,
            String detail,
            String zipCode,
            String tag,
            boolean isDefault
    ) {
        public static AddressItem from(AddressEntry entry) {
            var addr = entry.getAddress();
            return new AddressItem(
                    entry.getAddressId(),
                    addr.receiver(),
                    addr.mobile(),
                    addr.province(),
                    addr.city(),
                    addr.district(),
                    addr.detail(),
                    addr.zipCode(),
                    entry.getTag().name(),
                    entry.isDefault()
            );
        }
    }

    /**
     * 从领域对象装配 DTO。
     */
    public static UserDetailResponse from(User user) {
        List<AddressItem> addressItems = user.getAddresses().stream()
                .map(AddressItem::from)
                .toList();
        return new UserDetailResponse(
                user.getUserId(),
                user.getNickname(),
                user.getAvatar(),
                user.getMobile().masked(),
                user.getEmail() == null ? null : user.getEmail().masked(),
                user.getGender().getCode(),
                user.getMemberLevel().name(),
                user.getStatus().name(),
                user.getRegisterTime(),
                user.getRoleIds(),
                addressItems
        );
    }
}
