package com.example.ddd.user.domain.model.entity;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Mobile;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.user.domain.model.valueobject.AddressTag;

import java.util.Objects;

/**
 * 实体 (Entity)：收货地址条目 AddressEntry。
 *
 * <p><b>为什么是实体而不是值对象？</b>
 * 虽然内部持有的 {@link Address} 是值对象（无标识、不可变），
 * 但"用户在地址簿中保存的一条地址"具有：
 * <ul>
 *   <li>唯一标识 addressId：可以引用、修改、删除单条地址。</li>
 *   <li>生命周期：用户可以增删改地址。</li>
 *   <li>额外元数据：tag、isDefault，这些不属于 Address 值对象。</li>
 * </ul>
 * 因此它是<b>实体</b>，且属于 {@link com.example.ddd.user.domain.model.aggregate.User} 聚合内部实体。</p>
 *
 * <p><b>为什么归入 User 聚合？</b>
 * "地址"离开了"用户"没有业务意义，二者的一致性由 User 聚合根守护：
 * <ul>
 *   <li>一个用户最多只能有一个默认地址（跨地址的不变式）。</li>
 *   <li>删除用户时级联删除地址。</li>
 *   <li>地址数量上限（防止恶意刷）。</li>
 * </ul>
 * 这些规则必须由聚合根统一执行，禁止外部直接操作 AddressEntry。</p>
 *
 * @author ddd-learning
 */
public class AddressEntry {

    /** 地址 ID（实体标识，聚合内唯一） */
    private final String addressId;

    /** 所属用户 ID（表明归属，不建立对象引用，避免聚合间硬耦合） */
    private final String userId;

    /** 地址内容（值对象，不可变） */
    private Address address;

    /** 标签 */
    private AddressTag tag;

    /** 是否默认地址 */
    private boolean isDefault;

    public AddressEntry(String addressId, String userId, Address address, AddressTag tag, boolean isDefault) {
        this.addressId = Objects.requireNonNull(addressId, "addressId 不能为 null");
        this.userId = Objects.requireNonNull(userId, "userId 不能为 null");
        this.address = Objects.requireNonNull(address, "address 不能为 null");
        this.tag = tag == null ? AddressTag.OTHER : tag;
        this.isDefault = isDefault;
    }

    /**
     * 修改地址内容（值对象整体替换，而不是修改字段）。
     */
    public void changeAddress(Address newAddress) {
        this.address = Objects.requireNonNull(newAddress, "新地址不能为 null");
    }

    /**
     * 修改标签。
     */
    public void changeTag(AddressTag newTag) {
        this.tag = newTag == null ? AddressTag.OTHER : newTag;
    }

    /**
     * 标记为默认地址。
     * <p><b>注意：</b>本方法只设置当前条目为默认，"取消其他默认"的不变式由 User 聚合根维护。
     * 由于 AddressEntry 与 User 位于不同的子包 (entity vs aggregate)，此处声明为 public；
     * 但语义上仍应"仅由 User 聚合根调用"，禁止应用服务/基础设施层直接调用。</p>
     */
    public void markAsDefault() {
        this.isDefault = true;
    }

    /**
     * 取消默认。
     * <p>同 {@link #markAsDefault()}，public 只为跨子包访问，语义上仅聚合根内部使用。</p>
     */
    public void unmarkDefault() {
        this.isDefault = false;
    }

    public String getAddressId() { return addressId; }
    public String getUserId() { return userId; }
    public Address getAddress() { return address; }
    public AddressTag getTag() { return tag; }
    public boolean isDefault() { return isDefault; }

    /**
     * 校验手机号与地址内手机号一致（可选业务规则）。
     */
    public void validateMobileConsistency(Mobile userMobile) {
        if (userMobile != null && !userMobile.value().equals(this.address.mobile())) {
            // 允许收件人手机号与账号手机号不同（例如给别人寄东西），此处仅示例校验点
            // 严格业务可在此抛异常
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddressEntry other)) return false;
        return addressId.equals(other.addressId);
    }

    @Override
    public int hashCode() {
        return addressId.hashCode();
    }
}
