package com.example.ddd.contract.user;

import java.io.Serializable;

/**
 * 收货地址 DTO。
 *
 * @param addressId 地址 ID
 * @param userId    所属用户
 * @param province  省
 * @param city      市
 * @param district  区
 * @param detail    详细地址
 * @param zipCode   邮编
 * @param receiver  收件人
 * @param mobile    收件人手机号
 * @param isDefault 是否默认地址
 */
public record AddressDTO(String addressId,
                         String userId,
                         String province,
                         String city,
                         String district,
                         String detail,
                         String zipCode,
                         String receiver,
                         String mobile,
                         boolean isDefault) implements Serializable {
}
