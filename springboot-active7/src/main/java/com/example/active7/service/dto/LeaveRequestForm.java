package com.example.active7.service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请表单；userId 等可信数据由服务端从登录信息获取。
 */
public class LeaveRequestForm {

    @NotBlank(message = "请选择请假类型")
    private String leaveType;

    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;

    @NotNull(message = "请填写请假天数")
    @DecimalMin(value = "0.5", message = "请假天数至少 0.5 天")
    @DecimalMax(value = "30.0", message = "单次请假最多 30 天")
    private BigDecimal leaveDays;

    @NotBlank(message = "请填写请假事由")
    @Size(max = 500, message = "请假事由最多 500 字")
    private String reason;

    private boolean hasMedicalProof;

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getLeaveDays() {
        return leaveDays;
    }

    public void setLeaveDays(BigDecimal leaveDays) {
        this.leaveDays = leaveDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isHasMedicalProof() {
        return hasMedicalProof;
    }

    public void setHasMedicalProof(boolean hasMedicalProof) {
        this.hasMedicalProof = hasMedicalProof;
    }
}
