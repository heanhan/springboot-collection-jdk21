package com.example.ddd.user.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.domain.valueobject.Email;
import com.example.ddd.common.domain.valueobject.Mobile;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.util.IdGenerator;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.domain.model.event.UserDisabledEvent;
import com.example.ddd.user.domain.model.event.UserProfileUpdatedEvent;
import com.example.ddd.user.domain.model.valueobject.AddressTag;
import com.example.ddd.user.domain.model.valueobject.Gender;
import com.example.ddd.user.domain.model.valueobject.MemberLevel;
import com.example.ddd.user.domain.model.valueobject.UserStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 聚合根 (Aggregate Root)：User 用户。
 *
 * <p><b>聚合边界：</b>
 * User 聚合包含：User 本身 + 多个 AddressEntry (收货地址簿条目)。
 * 之所以把地址簿纳入 User 聚合：
 * <ul>
 *   <li>"一个用户最多一个默认地址"是<b>跨条目的不变式</b>，必须由聚合根守护。</li>
 *   <li>地址数量上限（默认 20 条）也是聚合级规则。</li>
 *   <li>地址的生命周期完全依附于用户。</li>
 * </ul>
 *
 * <p><b>为什么不把 Role / Permission 放进 User 聚合？</b>
 * Role 是独立的聚合根，可以被多个用户共享，独立生命周期；
 * User 与 Role 的关联通过 t_user_role 关联表维护，属于"聚合间引用"，
 * 只保存对方的 ID 而非对象引用，避免加载一个 User 就把整个 RBAC 图拉起来。</p>
 *
 * <p><b>不变式 (Invariants)：</b>
 * <ol>
 *   <li>userId 一经创建不可修改。</li>
 *   <li>mobile 必须合法且业务上唯一（唯一性由 Repository + 数据库唯一索引共同保证）。</li>
 *   <li>status = DISABLED 时不允许修改资料。</li>
 *   <li>地址数量 ≤ 20；同一时刻最多一个 isDefault = true。</li>
 *   <li>memberLevel 只能通过 {@link #promoteMemberLevel(MemberLevel)} 升级，不允许降级（业务规则）。</li>
 * </ol>
 *
 * <p><b>生命周期：</b>
 * 注册（由 auth-service 通过 MQ 触发） → 完善资料 → 添加地址 → 升级会员 → 停用/删除。</p>
 *
 * @author ddd-learning
 */
public class User extends BaseAggregateRoot {

    /** 单用户地址数量上限 */
    public static final int MAX_ADDRESS_COUNT = 20;

    // ============================================================
    // 属性
    // ============================================================

    /** 用户 ID（聚合根标识，全局唯一） */
    private final String userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 手机号（值对象，构造时校验格式） */
    private Mobile mobile;

    /** 邮箱（值对象，可为 null） */
    private Email email;

    /** 性别 */
    private Gender gender;

    /** 会员等级 */
    private MemberLevel memberLevel;

    /** 用户状态 */
    private UserStatus status;

    /** 注册时间 */
    private final LocalDateTime registerTime;

    /** 收货地址簿（聚合内部实体集合） */
    private final List<AddressEntry> addresses = new ArrayList<>();

    /** 用户所拥有的角色 ID 集合（跨聚合引用，只保存 ID） */
    private final List<String> roleIds = new ArrayList<>();

    // ============================================================
    // 构造 / 工厂方法
    // ============================================================

    /**
     * 通过工厂方法创建新用户（推荐入口）。
     *
     * <p>为什么不直接暴露 public 构造器？</p>
     * <p>工厂方法可以：
     * <ul>
     *   <li>表达业务语义（{@code register} 比 {@code new User} 更清晰）。</li>
     *   <li>集中处理默认值（memberLevel = NORMAL, status = ACTIVE）。</li>
     *   <li>注册领域事件（UserRegisteredEvent）。</li>
     * </ul>
     */
    public static User register(String nickname, Mobile mobile, Email email, Gender gender) {
        Objects.requireNonNull(nickname, "昵称不能为 null");
        Objects.requireNonNull(mobile, "手机号不能为 null");
        String userId = IdGenerator.nextIdStr();
        User user = new User(userId, nickname, null, mobile, email,
                gender == null ? Gender.UNKNOWN : gender,
                MemberLevel.NORMAL, UserStatus.ACTIVE, LocalDateTime.now());
        // 注册"用户已注册"领域事件（应用服务在事务提交后发布到 MQ）
        user.registerEvent(new com.example.ddd.user.domain.model.event.UserRegisteredEvent(
                userId, nickname, mobile.value(),
                email == null ? null : email.value()));
        return user;
    }

    /**
     * 重建 (Reconstitute)：从持久化数据重建 User 对象。
     * <p>由 {@link com.example.ddd.user.domain.repository.UserRepository} 的实现调用，
     * 不注册任何领域事件（重建的是历史状态，不是新事件）。</p>
     */
    public static User reconstitute(String userId, String nickname, String avatar, Mobile mobile, Email email,
                                    Gender gender, MemberLevel memberLevel, UserStatus status,
                                    LocalDateTime registerTime, List<AddressEntry> addresses,
                                    List<String> roleIds) {
        User user = new User(userId, nickname, avatar, mobile, email, gender,
                memberLevel, status, registerTime);
        if (addresses != null) user.addresses.addAll(addresses);
        if (roleIds != null) user.roleIds.addAll(roleIds);
        return user;
    }

    private User(String userId, String nickname, String avatar, Mobile mobile, Email email,
                 Gender gender, MemberLevel memberLevel, UserStatus status, LocalDateTime registerTime) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.mobile = mobile;
        this.email = email;
        this.gender = gender;
        this.memberLevel = memberLevel;
        this.status = status;
        this.registerTime = registerTime;
    }

    // ============================================================
    // 业务行为：资料修改
    // ============================================================

    /**
     * 修改昵称与头像。
     */
    public void updateProfile(String newNickname, String newAvatar, Gender newGender) {
        assertActive();
        if (newNickname != null && !newNickname.isBlank()) {
            this.nickname = newNickname.trim();
        }
        if (newAvatar != null) {
            this.avatar = newAvatar;
        }
        if (newGender != null) {
            this.gender = newGender;
        }
        registerEvent(new UserProfileUpdatedEvent(this.userId));
    }

    /**
     * 修改手机号（业务上需要短信验证码校验，此处只做领域层规则）。
     */
    public void changeMobile(Mobile newMobile) {
        assertActive();
        Objects.requireNonNull(newMobile, "新手机号不能为 null");
        this.mobile = newMobile;
    }

    /**
     * 修改邮箱。
     */
    public void changeEmail(Email newEmail) {
        assertActive();
        this.email = newEmail;
    }

    /**
     * 停用账号。
     */
    public void disable(String reason) {
        if (this.status == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已处于停用状态");
        }
        this.status = UserStatus.DISABLED;
        registerEvent(new UserDisabledEvent(this.userId, reason));
    }

    /**
     * 启用账号。
     */
    public void enable() {
        if (this.status == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已处于启用状态");
        }
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 升级会员等级（不允许降级）。
     */
    public void promoteMemberLevel(MemberLevel newLevel) {
        Objects.requireNonNull(newLevel, "新等级不能为 null");
        if (!newLevel.isHigherThan(this.memberLevel)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "会员等级只能升不能降：当前 " + this.memberLevel + "，目标 " + newLevel);
        }
        this.memberLevel = newLevel;
    }

    // ============================================================
    // 业务行为：地址簿管理（跨条目不变式守护）
    // ============================================================

    /**
     * 新增收货地址。
     *
     * @param address      地址内容（值对象）
     * @param tag          标签
     * @param setAsDefault 是否设为默认；若为 true，则原有默认地址会被取消默认
     * @return 新建的 AddressEntry
     */
    public AddressEntry addAddress(Address address, AddressTag tag, boolean setAsDefault) {
        assertActive();
        Objects.requireNonNull(address, "地址不能为 null");

        // 不变式 1：地址数量上限
        if (this.addresses.size() >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.USER_ADDRESS_LIMIT_EXCEEDED,
                    "地址数量已达上限 " + MAX_ADDRESS_COUNT);
        }

        // 若是首条地址，强制设为默认
        boolean shouldBeDefault = setAsDefault || this.addresses.isEmpty();

        // 不变式 2：同一时刻最多一个默认地址
        if (shouldBeDefault) {
            this.addresses.forEach(AddressEntry::unmarkDefault);
        }

        AddressEntry entry = new AddressEntry(
                IdGenerator.nextIdStr(), this.userId, address, tag, shouldBeDefault);
        this.addresses.add(entry);
        return entry;
    }

    /**
     * 修改地址内容。
     */
    public void updateAddress(String addressId, Address newAddress, AddressTag newTag) {
        assertActive();
        AddressEntry entry = findAddressOrThrow(addressId);
        entry.changeAddress(newAddress);
        if (newTag != null) entry.changeTag(newTag);
    }

    /**
     * 删除地址。
     * <p>删除的是默认地址时，自动把剩下的第一条设为默认，维持"若有地址则必有默认"的不变式。</p>
     */
    public void removeAddress(String addressId) {
        assertActive();
        AddressEntry entry = findAddressOrThrow(addressId);
        boolean wasDefault = entry.isDefault();
        this.addresses.remove(entry);

        if (wasDefault && !this.addresses.isEmpty()) {
            this.addresses.get(0).markAsDefault();
        }
    }

    /**
     * 将某条地址设为默认。
     */
    public void markDefaultAddress(String addressId) {
        assertActive();
        AddressEntry target = findAddressOrThrow(addressId);
        this.addresses.forEach(AddressEntry::unmarkDefault);
        target.markAsDefault();
    }

    private AddressEntry findAddressOrThrow(String addressId) {
        return this.addresses.stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ADDRESS_NOT_FOUND,
                        "地址不存在: " + addressId));
    }

    // ============================================================
    // 业务行为：角色关联（跨聚合协作，只保存 ID）
    // ============================================================

    /**
     * 分配角色。
     */
    public void assignRole(String roleId) {
        Objects.requireNonNull(roleId, "roleId 不能为 null");
        if (!this.roleIds.contains(roleId)) {
            this.roleIds.add(roleId);
        }
    }

    /**
     * 移除角色。
     */
    public void removeRole(String roleId) {
        this.roleIds.remove(roleId);
    }

    // ============================================================
    // 内部校验
    // ============================================================

    private void assertActive() {
        if (this.status != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_PERMISSION_DENIED, "用户已停用，禁止修改");
        }
    }

    // ============================================================
    // Getters（不提供 Setters，强制通过业务方法修改）
    // ============================================================

    @Override
    public String aggregateId() {
        return this.userId;
    }

    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public Mobile getMobile() { return mobile; }
    public Email getEmail() { return email; }
    public Gender getGender() { return gender; }
    public MemberLevel getMemberLevel() { return memberLevel; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getRegisterTime() { return registerTime; }

    /** 只读视图，防止外部绕过聚合根修改 */
    public List<AddressEntry> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public List<String> getRoleIds() {
        return Collections.unmodifiableList(roleIds);
    }

    /** 获取默认地址 */
    public Optional<AddressEntry> getDefaultAddress() {
        return this.addresses.stream().filter(AddressEntry::isDefault).findFirst();
    }
}
