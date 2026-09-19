package com.example.ddd.inventory.application.command;

/**
 * 命令：创建仓库。
 *
 * @param code     仓库编码（唯一）
 * @param name     仓库名
 * @param province 省
 * @param city     市
 * @param district 区
 * @param address  详细地址
 * @param contact  联系人
 * @param phone    联系电话
 * @param priority 发货优先级（数字越小越优先）
 */
public record CreateWarehouseCommand(String code, String name, String province, String city,
                                     String district, String address, String contact, String phone,
                                     int priority) {
}
