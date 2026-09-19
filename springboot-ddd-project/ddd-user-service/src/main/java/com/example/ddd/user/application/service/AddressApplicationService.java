package com.example.ddd.user.application.service;

import com.example.ddd.common.domain.valueobject.Address;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.user.application.command.SaveAddressCommand;
import com.example.ddd.user.domain.model.aggregate.User;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.domain.model.valueobject.AddressTag;
import com.example.ddd.user.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用服务：地址簿管理用例。
 *
 * <p><b>为什么单独一个应用服务而不是并入 UserApplicationService？</b>
 * 单一职责。地址簿的用例足够多（增删改查、设默认），且业务上属于同一个"聚合根 User 的子领域"，
 * 拆出来让 UserApplicationService 保持精简。</p>
 *
 * <p><b>核心不变式守护：</b>
 * 全部通过 {@link User} 聚合根的方法调用，禁止绕过聚合根直接操作 AddressEntry。</p>
 *
 * @author ddd-learning
 */
@Service
public class AddressApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AddressApplicationService.class);

    private final UserRepository userRepository;

    public AddressApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 用例：新增或修改地址。
     *
     * <p>若 {@code addressId} 为空视为新增，否则视为修改。</p>
     *
     * @return 地址 ID
     */
    @Transactional
    public String saveAddress(SaveAddressCommand cmd) {
        User user = loadUser(cmd.userId());
        Address address = new Address(cmd.province(), cmd.city(), cmd.district(),
                cmd.detail(), cmd.zipCode(), cmd.receiver(), cmd.mobile());
        AddressTag tag = parseTag(cmd.tag());

        if (cmd.addressId() == null || cmd.addressId().isBlank()) {
            AddressEntry entry = user.addAddress(address, tag, cmd.setAsDefault());
            userRepository.save(user);
            log.info("[Address] added userId={} addressId={}", cmd.userId(), entry.getAddressId());
            return entry.getAddressId();
        } else {
            user.updateAddress(cmd.addressId(), address, tag);
            if (cmd.setAsDefault()) {
                user.markDefaultAddress(cmd.addressId());
            }
            userRepository.save(user);
            return cmd.addressId();
        }
    }

    /**
     * 用例：删除地址。
     */
    @Transactional
    public void removeAddress(String userId, String addressId) {
        User user = loadUser(userId);
        user.removeAddress(addressId);
        userRepository.save(user);
    }

    /**
     * 用例：设为默认地址。
     */
    @Transactional
    public void markDefault(String userId, String addressId) {
        User user = loadUser(userId);
        user.markDefaultAddress(addressId);
        userRepository.save(user);
    }

    /**
     * 用例：查询用户所有地址。
     */
    @Transactional(readOnly = true)
    public List<AddressEntry> listAddresses(String userId) {
        return loadUser(userId).getAddresses();
    }

    /**
     * 用例：查询默认地址。
     */
    @Transactional(readOnly = true)
    public AddressEntry getDefaultAddress(String userId) {
        return loadUser(userId).getDefaultAddress().orElse(null);
    }

    /**
     * 用例：根据 addressId 精确查询（含越权校验）。
     */
    @Transactional(readOnly = true)
    public AddressEntry getAddress(String userId, String addressId) {
        return loadUser(userId).getAddresses().stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ADDRESS_NOT_FOUND,
                        "地址不存在：" + addressId));
    }

    private User loadUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "用户不存在：" + userId));
    }

    private AddressTag parseTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return AddressTag.OTHER;
        }
        try {
            return AddressTag.valueOf(tag.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AddressTag.OTHER;
        }
    }
}
