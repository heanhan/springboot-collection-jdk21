package com.example.ddd.user.application.command;

/**
 * 命令：新增/修改收货地址。
 *
 * @param userId       用户 ID
 * @param addressId    地址 ID（修改时非空，新增时为 null）
 * @param receiver     收件人
 * @param mobile       收件人手机号
 * @param province     省
 * @param city         市
 * @param district     区
 * @param detail       详细地址
 * @param zipCode      邮编
 * @param tag          标签 HOME/COMPANY/SCHOOL/OTHER
 * @param setAsDefault 是否设为默认
 */
public record SaveAddressCommand(String userId,
                                 String addressId,
                                 String receiver,
                                 String mobile,
                                 String province,
                                 String city,
                                 String district,
                                 String detail,
                                 String zipCode,
                                 String tag,
                                 boolean setAsDefault) {
}
