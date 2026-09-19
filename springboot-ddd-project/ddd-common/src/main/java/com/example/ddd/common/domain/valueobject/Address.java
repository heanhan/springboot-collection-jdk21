package com.example.ddd.common.domain.valueobject;

import java.util.Objects;

/**
 * 值对象：地址 Address。
 *
 * <p><b>为什么是值对象？</b>
 * 一段"省-市-区-详细"信息本身没有业务标识，两个内容完全相同的地址在业务上等价。
 * 但在电商场景，用户往往需要保存多个"收货地址"并对每个地址命名（家、公司），
 * 此时"地址簿条目"变成了实体（有 addressId、可修改），
 * 而条目内部持有的"地址内容"仍然是值对象。</p>
 *
 * <p><b>典型分层：</b>
 * <pre>
 *   User (聚合根)
 *     └── AddressBookEntry (实体，有 addressId，可标记默认)
 *           └── Address (值对象，纯内容，不可变)
 * </pre>
 *
 * @param province   省，例如 "广东省"
 * @param city       市，例如 "深圳市"
 * @param district   区/县，例如 "南山区"
 * @param detail     详细地址，例如 "科技园 xx 大厦 yy 楼"
 * @param zipCode    邮政编码，可为 null
 * @param receiver   收件人姓名
 * @param mobile     收件人手机号
 * @author ddd-learning
 */
public record Address(String province,
                      String city,
                      String district,
                      String detail,
                      String zipCode,
                      String receiver,
                      String mobile) {

    /**
     * 紧凑构造器：非空校验 + trim。
     */
    public Address {
        province = requireNonBlank(province, "province");
        city = requireNonBlank(city, "city");
        district = requireNonBlank(district, "district");
        detail = requireNonBlank(detail, "detail");
        receiver = requireNonBlank(receiver, "receiver");
        mobile = requireNonBlank(mobile, "mobile");
        // 邮编允许为空，非空时 trim
        zipCode = zipCode == null ? null : zipCode.trim();
    }

    /**
     * 拼装完整地址字符串（不含收件人）。
     */
    public String fullAddress() {
        return province + city + district + detail;
    }

    /**
     * 生成脱敏后的展示字符串，用于日志或前端。
     */
    public String maskedDisplay() {
        return province + city + district + "****" + " / " + receiver.charAt(0) + "**" + " / " + maskMobile(mobile);
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return "***";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " 不能为 null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白字符串");
        }
        return trimmed;
    }
}
