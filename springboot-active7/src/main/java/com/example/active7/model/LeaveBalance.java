package com.example.active7.model;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 员工年度假期余额。
 */
@TableName("leave_balance")
public class LeaveBalance {

    private String userId;
    private int balanceYear;
    private BigDecimal annualDays;
    private BigDecimal usedDays;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getBalanceYear() {
        return balanceYear;
    }

    public void setBalanceYear(int balanceYear) {
        this.balanceYear = balanceYear;
    }

    public BigDecimal getAnnualDays() {
        return annualDays;
    }

    public void setAnnualDays(BigDecimal annualDays) {
        this.annualDays = annualDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(BigDecimal usedDays) {
        this.usedDays = usedDays;
    }
}
