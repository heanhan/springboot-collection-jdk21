package com.example.ddd.user.infrastructure.persistence.po;

import com.example.ddd.common.infrastructure.persistence.AbstractJpaAuditablePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PO：收货地址表（User 聚合内实体）。
 */
@Entity
@Table(name = "t_user_address")
public class UserAddressPO extends AbstractJpaAuditablePO {

    @Id
    @Column(name = "address_id", length = 32, nullable = false)
    private String addressId;

    @Column(name = "user_id", length = 32, nullable = false)
    private String userId;

    @Column(name = "receiver", length = 64, nullable = false)
    private String receiver;

    @Column(name = "mobile", length = 20, nullable = false)
    private String mobile;

    @Column(name = "province", length = 32, nullable = false)
    private String province;

    @Column(name = "city", length = 32, nullable = false)
    private String city;

    @Column(name = "district", length = 32, nullable = false)
    private String district;

    @Column(name = "detail", length = 255, nullable = false)
    private String detail;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "tag", length = 16)
    private String tag;

    @Column(name = "is_default", nullable = false)
    private Integer isDefault;

    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
}
