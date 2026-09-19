package com.example.ddd.user.interfaces.assembler;

import com.example.ddd.contract.user.AddressDTO;
import com.example.ddd.contract.user.UserDTO;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;

/**
 * 装配器 (Assembler)：Domain -> 契约 DTO（发布语言）。
 *
 * <p><b>Assembler vs Converter：</b>
 * <ul>
 *   <li>Converter：PO &lt;-&gt; Domain（infrastructure 层，跨"技术-业务"边界）。</li>
 *   <li>Assembler：Domain -&gt; DTO（interfaces 层，跨"业务-外部"边界）。</li>
 * </ul>
 * 二者职责不同，命名区分有助于快速定位。</p>
 */
public final class UserAssembler {

    private UserAssembler() {
    }

    /**
     * Domain User -> 契约 UserDTO（供其他服务通过 Feign 消费）。
     */
    public static UserDTO toContractDTO(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getNickname(),
                user.getAvatar(),
                user.getMobile().masked(),
                user.getMemberLevel().name(),
                user.getStatus().name()
        );
    }

    /**
     * Domain AddressEntry -> 契约 AddressDTO。
     */
    public static AddressDTO toContractDTO(AddressEntry entry) {
        if (entry == null) return null;
        var addr = entry.getAddress();
        return new AddressDTO(
                entry.getAddressId(),
                entry.getUserId(),
                addr.province(),
                addr.city(),
                addr.district(),
                addr.detail(),
                addr.zipCode(),
                addr.receiver(),
                addr.mobile(),
                entry.isDefault()
        );
    }
}
