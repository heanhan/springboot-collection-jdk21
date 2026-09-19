package com.example.ddd.user.infrastructure.persistence.converter;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Email;
import com.example.ddd.common.domain.valueobject.Mobile;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.domain.model.valueobject.AddressTag;
import com.example.ddd.user.domain.model.valueobject.Gender;
import com.example.ddd.user.domain.model.valueobject.MemberLevel;
import com.example.ddd.user.domain.model.valueobject.UserStatus;
import com.example.ddd.user.infrastructure.persistence.po.UserAddressPO;
import com.example.ddd.user.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 转换器 (Converter)：User 领域模型 &lt;-&gt; UserPO / UserAddressPO。
 *
 * <p><b>为什么手写而不用 MapStruct？</b>
 * 学习项目为了展示"聚合根重建 (reconstitute)"的完整过程，手写更清晰。
 * 生产项目字段多时用 MapStruct 可以省很多样板代码。</p>
 *
 * <p><b>转换的两个方向：</b>
 * <ul>
 *   <li>{@link #toDomain}：PO -> Domain，用 {@link User#reconstitute} 而非 {@link User#register}，
 *       避免误发领域事件。</li>
 *   <li>{@link #toPO}：Domain -> PO，把值对象拆回字符串，供 JPA 持久化。</li>
 * </ul>
 */
@Component
public class UserConverter {

    // ============================================================
    // PO -> Domain (reconstitute)
    // ============================================================

    public User toDomain(UserPO po, List<UserAddressPO> addressPOs, List<String> roleIds) {
        List<AddressEntry> entries = addressPOs == null ? List.of() : addressPOs.stream()
                .map(this::toAddressEntry)
                .toList();
        return User.reconstitute(
                po.getUserId(),
                po.getNickname(),
                po.getAvatar(),
                new Mobile(po.getMobile()),
                po.getEmail() == null ? null : new Email(po.getEmail()),
                Gender.fromCode(po.getGender()),
                MemberLevel.valueOf(po.getMemberLevel()),
                UserStatus.valueOf(po.getStatus()),
                po.getRegisterTime(),
                entries,
                roleIds
        );
    }

    public AddressEntry toAddressEntry(UserAddressPO po) {
        Address address = new Address(po.getProvince(), po.getCity(), po.getDistrict(),
                po.getDetail(), po.getZipCode(), po.getReceiver(), po.getMobile());
        AddressTag tag = po.getTag() == null ? AddressTag.OTHER : AddressTag.valueOf(po.getTag());
        return new AddressEntry(po.getAddressId(), po.getUserId(), address, tag,
                po.getIsDefault() != null && po.getIsDefault() == 1);
    }

    // ============================================================
    // Domain -> PO
    // ============================================================

    public UserPO toPO(User user) {
        UserPO po = new UserPO();
        po.setUserId(user.getUserId());
        po.setNickname(user.getNickname());
        po.setAvatar(user.getAvatar());
        po.setMobile(user.getMobile().value());
        po.setEmail(user.getEmail() == null ? null : user.getEmail().value());
        po.setGender(user.getGender().getCode());
        po.setMemberLevel(user.getMemberLevel().name());
        po.setStatus(user.getStatus().name());
        po.setRegisterTime(user.getRegisterTime());
        return po;
    }

    public UserAddressPO toAddressPO(AddressEntry entry) {
        UserAddressPO po = new UserAddressPO();
        po.setAddressId(entry.getAddressId());
        po.setUserId(entry.getUserId());
        Address addr = entry.getAddress();
        po.setReceiver(addr.receiver());
        po.setMobile(addr.mobile());
        po.setProvince(addr.province());
        po.setCity(addr.city());
        po.setDistrict(addr.district());
        po.setDetail(addr.detail());
        po.setZipCode(addr.zipCode());
        po.setTag(entry.getTag().name());
        po.setIsDefault(entry.isDefault() ? 1 : 0);
        return po;
    }
}
