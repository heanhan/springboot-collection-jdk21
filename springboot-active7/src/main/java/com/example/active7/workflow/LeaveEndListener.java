package com.example.active7.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.active7.mapper.LeaveBalanceMapper;
import com.example.active7.mapper.LeaveRecordMapper;
import com.example.active7.mapper.LeaveRequestMapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.model.LeaveRecord;
import com.example.active7.model.LeaveRequest;
import com.example.active7.model.SysUser;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class LeaveEndListener implements JavaDelegate {

    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final SysUserMapper sysUserMapper;

    public LeaveEndListener(LeaveRequestMapper leaveRequestMapper, LeaveRecordMapper leaveRecordMapper,
                            LeaveBalanceMapper leaveBalanceMapper, SysUserMapper sysUserMapper) {
        this.leaveRequestMapper = leaveRequestMapper;
        this.leaveRecordMapper = leaveRecordMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String requestId = execution.getProcessInstanceBusinessKey();
        String outcome = (String) execution.getVariable("outcome");
        String lastComment = (String) execution.getVariable("lastComment");
        if (outcome == null || outcome.trim().isEmpty()) {
            outcome = LeaveProcessConstants.STATUS_REJECTED;
        }

        List<String> finalStatuses = Arrays.asList(LeaveProcessConstants.STATUS_APPROVED,
                LeaveProcessConstants.STATUS_REJECTED, LeaveProcessConstants.STATUS_WITHDRAWN);
        int updated = leaveRequestMapper.update(null, new LambdaUpdateWrapper<LeaveRequest>()
                .eq(LeaveRequest::getId, requestId)
                .notIn(LeaveRequest::getStatus, finalStatuses)
                .set(LeaveRequest::getStatus, outcome)
                .set(LeaveRequest::getCurrentNode, LeaveProcessConstants.NODE_END)
                .set(LeaveRequest::getLastComment, lastComment)
                .set(LeaveRequest::getUpdatedAt, LocalDateTime.now()));

        if (updated == 0) {
            return;
        }

        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (LeaveProcessConstants.STATUS_APPROVED.equals(outcome)
                && LeaveProcessConstants.TYPE_ANNUAL.equals(request.getLeaveType())) {
            SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.getApplicant()));
            int balanceUpdated = leaveBalanceMapper.addUsedDays(user.getUserId(),
                    LocalDate.now().getYear(), request.getLeaveDays());
            if (balanceUpdated == 0) {
                throw new IllegalStateException("年假余额不足，流程回滚，请联系 HR 核对余额");
            }
        }

        insertRecord(requestId, LeaveProcessConstants.NODE_END, "END", "SYSTEM", lastComment);
    }

    private void insertRecord(String requestId, String node, String action,
                              String operator, String comment) {
        LeaveRecord record = new LeaveRecord();
        record.setId(UUID.randomUUID().toString());
        record.setRequestId(requestId);
        record.setNode(node);
        record.setAction(action);
        record.setOperator(operator);
        record.setComment(comment);
        leaveRecordMapper.insert(record);
    }
}
