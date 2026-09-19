package com.example.ddd.inventory.domain.model.aggregate;

import com.example.ddd.common.domain.model.BaseAggregateRoot;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;

import java.util.Objects;

/**
 * 聚合根：Warehouse 仓库。
 *
 * <p><b>为什么不把 Stock 放在 Warehouse 聚合内？</b>
 * 见 {@link Stock} 类注释：并发扣减 + 数据规模决定 Stock 必须独立聚合。</p>
 *
 * <p><b>Warehouse 只管理"仓库本身的元数据"：</b>
 * code、name、address、priority、status。</p>
 */
public class Warehouse extends BaseAggregateRoot {

    private final String warehouseId;
    private String code;
    private String name;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contact;
    private String phone;
    private int priority;
    private boolean enabled;

    public static Warehouse create(String warehouseId, String code, String name,
                                   String province, String city, String district, String address,
                                   String contact, String phone, int priority) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仓库编码不能为空");
        }
        Warehouse w = new Warehouse(warehouseId);
        w.code = code;
        w.name = name;
        w.province = province;
        w.city = city;
        w.district = district;
        w.address = address;
        w.contact = contact;
        w.phone = phone;
        w.priority = priority;
        w.enabled = true;
        return w;
    }

    public static Warehouse reconstitute(String warehouseId, String code, String name,
                                         String province, String city, String district, String address,
                                         String contact, String phone, int priority, boolean enabled) {
        Warehouse w = new Warehouse(warehouseId);
        w.code = code;
        w.name = name;
        w.province = province;
        w.city = city;
        w.district = district;
        w.address = address;
        w.contact = contact;
        w.phone = phone;
        w.priority = priority;
        w.enabled = enabled;
        return w;
    }

    private Warehouse(String warehouseId) {
        this.warehouseId = Objects.requireNonNull(warehouseId);
    }

    public void updateInfo(String name, String province, String city, String district,
                           String address, String contact, String phone, Integer priority) {
        if (name != null) this.name = name;
        if (province != null) this.province = province;
        if (city != null) this.city = city;
        if (district != null) this.district = district;
        if (address != null) this.address = address;
        if (contact != null) this.contact = contact;
        if (phone != null) this.phone = phone;
        if (priority != null) this.priority = priority;
    }

    public void disable() { this.enabled = false; }
    public void enable() { this.enabled = true; }

    @Override
    public String aggregateId() { return warehouseId; }

    public String getWarehouseId() { return warehouseId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getAddress() { return address; }
    public String getContact() { return contact; }
    public String getPhone() { return phone; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }
}
