package com.example.active7.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录人能看到的一个 Active7 任务及其请假详情。
 */
public class TaskView {

    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String taskDefinitionKeyLabel;
    private LocalDateTime createTime;
    private String assignee;
    private String requestId;
    private String applicantName;
    private String department;
    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal leaveDays;
    private String reason;
    private boolean hasMedicalProof;
    private String status;
    private String currentNode;
    private List<LeaveRecordView> records;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getTaskDefinitionKeyLabel() {
        return taskDefinitionKeyLabel;
    }

    public void setTaskDefinitionKeyLabel(String taskDefinitionKeyLabel) {
        this.taskDefinitionKeyLabel = taskDefinitionKeyLabel;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public List<LeaveRecordView> getRecords() {
        return records;
    }

    public void setRecords(List<LeaveRecordView> records) {
        this.records = records;
    }
}
