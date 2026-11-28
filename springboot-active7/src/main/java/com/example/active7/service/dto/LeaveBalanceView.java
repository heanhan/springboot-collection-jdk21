package com.example.active7.service.dto;

import java.math.BigDecimal;

/**
 * 页面展示用年度假期余额。
 */
public class LeaveBalanceView {

    private int balanceYear;
    private BigDecimal annualDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;

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

    public BigDecimal getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(BigDecimal remainingDays) {
        this.remainingDays = remainingDays;
    }
}
