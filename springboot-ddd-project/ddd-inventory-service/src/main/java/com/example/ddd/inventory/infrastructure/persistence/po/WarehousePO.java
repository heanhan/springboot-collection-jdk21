package com.example.ddd.inventory.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：t_warehouse 表映射（Warehouse 聚合根）。
 *
 * <p>领域模型用 {@code boolean enabled}，PO 用 {@code status} 字符串（ACTIVE/DISABLED），
 * 由 Converter 双向映射。</p>
 */
@Entity
@Table(name = "t_warehouse")
public class WarehousePO extends AbstractJpaAuditablePO {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    @Id
    @Column(name = "warehouse_id", length = 32, nullable = false)
    private String warehouseId;

    @Column(name = "code", length = 32, nullable = false)
    private String code;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "province", length = 32)
    private String province;

    @Column(name = "city", length = 32)
    private String city;

    @Column(name = "district", length = 32)
    private String district;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "contact", length = 64)
    private String contact;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
