package com.example.active7.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.active7.mapper.LeaveBalanceMapper;
import com.example.active7.mapper.LeaveRecordMapper;
import com.example.active7.mapper.LeaveRequestMapper;
import com.example.active7.mapper.SysUserMapper;
import com.example.active7.model.LeaveBalance;
import com.example.active7.model.LeaveRecord;
import com.example.active7.model.LeaveRequest;
import com.example.active7.model.SysUser;
import com.example.active7.security.RbacPermissions;
import com.example.active7.service.dto.ApprovalForm;
import com.example.active7.service.dto.LeaveBalanceView;
import com.example.active7.service.dto.LeaveRecordView;
import com.example.active7.service.dto.LeaveRequestForm;
import com.example.active7.service.dto.TaskView;
import com.example.active7.workflow.LeaveProcessConstants;
import com.example.common.exceptins.BizException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 请假用例编排：同时写业务表、Active7 运行时表和业务轨迹。
 */
@Service
@Transactional
public class LeaveService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final SysUserMapper sysUserMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public LeaveService(LeaveRequestMapper leaveRequestMapper, LeaveRecordMapper leaveRecordMapper,
                        LeaveBalanceMapper leaveBalanceMapper, SysUserMapper sysUserMapper,
                        RuntimeService runtimeService, TaskService taskService) {
        this.leaveRequestMapper = leaveRequestMapper;
        this.leaveRecordMapper = leaveRecordMapper;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.sysUserMapper = sysUserMapper;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    /**
     * 员工提交请假：业务表和流程实例在同一个本地事务中创建。
     */
    public String submit(LeaveRequestForm form) {
        validateForm(form);
        String username = currentUsername();
        SysUser user = loadUser(username);

        LeaveRequest request = new LeaveRequest();
        request.setId(UUID.randomUUID().toString());
        request.setApplicant(username);
        request.setApplicantName(user.getDisplayName());
        request.setDepartment(user.getDepartment());
        request.setLeaveType(form.getLeaveType());
        request.setStartTime(form.getStartTime());
        request.setEndTime(form.getEndTime());
        request.setLeaveDays(form.getLeaveDays());
        request.setReason(form.getReason().trim());
        request.setHasMedicalProof(form.isHasMedicalProof());
        request.setStatus(LeaveProcessConstants.STATUS_SUBMITTED);
        request.setCurrentNode(LeaveProcessConstants.NODE_MANAGER);

        leaveRequestMapper.insert(request);

        Map<String, Object> variables = processVariables(request);
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                LeaveProcessConstants.PROCESS_KEY, request.getId(), variables).getId();

        leaveRequestMapper.update(null, new LambdaUpdateWrapper<LeaveRequest>()
                .eq(LeaveRequest::getId, request.getId())
                .set(LeaveRequest::getProcessInstanceId, processInstanceId));
        insertRecord(request.getId(), LeaveProcessConstants.NODE_MANAGER,
                "SUBMIT", username, "提交请假申请");
        return request.getId();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> listMine() {
        String username = currentUsername();
        return leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getApplicant, username)
                .orderByDesc(LeaveRequest::getCreatedAt));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> listAll() {
        return leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .orderByDesc(LeaveRequest::getCreatedAt));
    }

    @Transactional(readOnly = true)
    public LeaveRequest getMine(String requestId) {
        LeaveRequest request = findRequest(requestId);
        if (!request.getApplicant().equals(currentUsername()) && !hasViewAllPermission()) {
            throw new BizException(403, "只能查看自己的请假申请");
        }
        return request;
    }

    @Transactional(readOnly = true)
    public LeaveBalanceView balance() {
        String username = currentUsername();
        SysUser user = loadUser(username);
        int year = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceMapper.selectOne(new LambdaQueryWrapper<LeaveBalance>()
                .eq(LeaveBalance::getUserId, user.getUserId())
                .eq(LeaveBalance::getBalanceYear, year));
        LeaveBalanceView view = new LeaveBalanceView();
        view.setBalanceYear(year);
        view.setAnnualDays(balance.getAnnualDays());
        view.setUsedDays(balance.getUsedDays());
        view.setRemainingDays(view.getAnnualDays().subtract(view.getUsedDays()));
        return view;
    }

    /**
     * taskCandidateOrAssigned 是任务隔离的核心：
     * 候选组任务或已经签收给自己的任务，才会出现在当前账号的待办中。
     */
    @Transactional(readOnly = true)
    public List<TaskView> listMyTasks() {
        String username = currentUsername();
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(username)
                .active()
                .orderByTaskCreateTime()
                .desc()
                .list();
        List<TaskView> views = new ArrayList<TaskView>();
        for (Task task : tasks) {
            views.add(toTaskView(task));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public TaskView taskDetail(String taskId) {
        return toTaskView(visibleTask(taskId));
    }

    public void claim(String taskId) {
        String username = currentUsername();
        Task task = visibleTask(taskId);
        if (task.getAssignee() == null) {
            taskService.setAssignee(taskId, username);
            insertRecord(requestId(task), task.getTaskDefinitionKey(), "CLAIM", username, "签收任务");
        } else if (!username.equals(task.getAssignee())) {
            throw new BizException(403, "任务已经被其他账号签收");
        }
    }

    /**
     * 同意、拒绝或退回。变量名必须与 BPMN 中的条件表达式完全一致。
     */
    public void approve(String taskId, ApprovalForm form) {
        String username = currentUsername();
        Task task = visibleTask(taskId);
        String requestId = requestId(task);
        LeaveRequest request = findRequest(requestId);

        if (!form.isApproved() && (form.getComment() == null || form.getComment().trim().isEmpty())) {
            throw new BizException(400, "拒绝时必须填写审批意见");
        }
        if (form.isReturnToApplicant()
                && !LeaveProcessConstants.NODE_MANAGER.equals(task.getTaskDefinitionKey())) {
            throw new BizException(400, "只有部门经理可以退回补充");
        }
        if (form.isReturnToApplicant()
                && (form.getComment() == null || form.getComment().trim().isEmpty())) {
            throw new BizException(400, "退回时必须说明需要补充的内容");
        }
        if (task.getAssignee() != null && !username.equals(task.getAssignee())) {
            throw new BizException(403, "任务已经被其他账号签收");
        }

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("lastComment", form.getComment() == null ? "" : form.getComment().trim());

        String action;
        if (form.isReturnToApplicant()) {
            variables.put("returnToEmployee", true);
            variables.put("approveManager", false);
            action = "RETURN";
        } else if (LeaveProcessConstants.NODE_MANAGER.equals(task.getTaskDefinitionKey())) {
            variables.put("approveManager", form.isApproved());
            variables.put("returnToEmployee", false);
            variables.put("needHr", needsHr(request));
            variables.put("needDirector", request.getLeaveDays().doubleValue() >= 5.0D);
            if (!form.isApproved()) {
                variables.put("outcome", LeaveProcessConstants.STATUS_REJECTED);
            }
            action = form.isApproved() ? "APPROVE" : "REJECT";
        } else if (LeaveProcessConstants.NODE_HR.equals(task.getTaskDefinitionKey())) {
            variables.put("hrApproved", form.isApproved());
            if (!form.isApproved()) {
                variables.put("outcome", LeaveProcessConstants.STATUS_REJECTED);
            }
            action = form.isApproved() ? "APPROVE" : "REJECT";
        } else if (LeaveProcessConstants.NODE_DIRECTOR.equals(task.getTaskDefinitionKey())) {
            variables.put("directorApproved", form.isApproved());
            if (!form.isApproved()) {
                variables.put("outcome", LeaveProcessConstants.STATUS_REJECTED);
            }
            action = form.isApproved() ? "APPROVE" : "REJECT";
        } else if (LeaveProcessConstants.NODE_CONFIRM.equals(task.getTaskDefinitionKey())) {
            variables.put("employeeConfirmed", true);
            variables.put("outcome", LeaveProcessConstants.STATUS_APPROVED);
            action = "CONFIRM";
        } else {
            throw new BizException(400, "当前节点不支持此审批操作");
        }

        completeTask(task, username, variables, action, form.getComment());
    }

    /**
     * 经理退回后，由申请人修改内容并重新进入经理审批节点。
     */
    public void resubmit(String taskId, LeaveRequestForm form) {
        String username = currentUsername();
        Task task = visibleTask(taskId);
        if (!LeaveProcessConstants.NODE_RESUBMIT.equals(task.getTaskDefinitionKey())) {
            throw new BizException(400, "只有退回补充任务可以重新提交");
        }
        if (!username.equals(task.getAssignee())) {
            throw new BizException(403, "只能处理自己的补充任务");
        }
        validateForm(form);
        String requestId = requestId(task);
        LeaveRequest request = findRequest(requestId);
        if (!request.getApplicant().equals(username)) {
            throw new BizException(403, "只能修改自己的请假申请");
        }

        request.setLeaveType(form.getLeaveType());
        request.setStartTime(form.getStartTime());
        request.setEndTime(form.getEndTime());
        request.setLeaveDays(form.getLeaveDays());
        request.setReason(form.getReason().trim());
        request.setHasMedicalProof(form.isHasMedicalProof());
        request.setStatus(LeaveProcessConstants.STATUS_SUBMITTED);
        request.setUpdatedAt(LocalDateTime.now());
        leaveRequestMapper.updateById(request);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("leaveType", request.getLeaveType());
        variables.put("leaveDays", request.getLeaveDays().doubleValue());
        variables.put("hasMedicalProof", request.isHasMedicalProof());
        variables.put("returnToEmployee", false);
        variables.put("approveManager", false);
        variables.put("hrApproved", false);
        variables.put("directorApproved", false);
        variables.put("needHr", needsHr(request));
        variables.put("needDirector", request.getLeaveDays().doubleValue() >= 5.0D);
        variables.put("lastComment", "员工已补充并重新提交");

        completeTask(task, username, variables, "RESUBMIT", "员工已补充并重新提交");
    }

    /**
     * 未签收的经理任务允许申请人撤回；已签收任务需要先由经理处理。
     */
    public void withdraw(String requestId) {
        String username = currentUsername();
        LeaveRequest request = findRequest(requestId);
        if (!request.getApplicant().equals(username)) {
            throw new BizException(403, "只能撤回自己的请假申请");
        }
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .active()
                .list();
        if (tasks.size() != 1) {
            throw new BizException(400, "当前流程状态不允许撤回");
        }
        Task task = tasks.get(0);
        boolean withdrawable = LeaveProcessConstants.NODE_MANAGER.equals(task.getTaskDefinitionKey())
                || LeaveProcessConstants.NODE_RESUBMIT.equals(task.getTaskDefinitionKey());
        if (!withdrawable) {
            throw new BizException(400, "只有经理审批前或退回补充时可以撤回");
        }
        if (task.getAssignee() != null
                && LeaveProcessConstants.NODE_MANAGER.equals(task.getTaskDefinitionKey())) {
            throw new BizException(400, "经理已经签收任务，暂时不能撤回");
        }

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("approveManager", false);
        variables.put("returnToEmployee", false);
        variables.put("outcome", LeaveProcessConstants.STATUS_WITHDRAWN);
        variables.put("lastComment", "员工主动撤回");
        completeTask(task, username, variables, "WITHDRAW", "员工主动撤回");
    }

    @Transactional(readOnly = true)
    public List<LeaveRecordView> records(String requestId) {
        getMine(requestId);
        List<LeaveRecord> records = leaveRecordMapper.selectList(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getRequestId, requestId)
                .orderByAsc(LeaveRecord::getRecordSeq));
        List<LeaveRecordView> views = new ArrayList<LeaveRecordView>();
        for (LeaveRecord record : records) {
            LeaveRecordView view = new LeaveRecordView();
            view.setId(record.getId());
            view.setRequestId(record.getRequestId());
            view.setNode(record.getNode());
            view.setAction(record.getAction());
            view.setOperator(record.getOperator());
            view.setComment(record.getComment());
            view.setCreatedAt(record.getCreatedAt());
            views.add(view);
        }
        return views;
    }

    private void completeTask(Task task, String username, Map<String, Object> variables,
                              String action, String comment) {
        if (task.getAssignee() == null) {
            taskService.setAssignee(task.getId(), username);
        }

        insertRecord(requestId(task), task.getTaskDefinitionKey(), action, username, comment);
        String processInstanceId = task.getProcessInstanceId();
        if (comment != null && !comment.trim().isEmpty()) {
            taskService.addComment(task.getId(), null, comment.trim());
        }
        taskService.setVariables(task.getId(), variables);
        runtimeService.setVariables(processInstanceId, variables);
        taskService.complete(task.getId(), variables);
        refreshBusinessState(processInstanceId, comment);
    }

    private void refreshBusinessState(String processInstanceId, String comment) {
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
        if (activeTasks.isEmpty()) {
            return;
        }
        Task nextTask = activeTasks.get(0);
        String requestId = (String) runtimeService.getVariable(
                nextTask.getExecutionId(), "requestId");
        leaveRequestMapper.update(null, new LambdaUpdateWrapper<LeaveRequest>()
                .eq(LeaveRequest::getId, requestId)
                .set(LeaveRequest::getCurrentNode, nextTask.getTaskDefinitionKey())
                .set(LeaveRequest::getStatus, statusForNode(nextTask.getTaskDefinitionKey()))
                .set(LeaveRequest::getLastComment, comment)
                .set(LeaveRequest::getUpdatedAt, LocalDateTime.now()));
    }

    private Task visibleTask(String taskId) {
        String username = currentUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null) {
            throw new BizException(404, "任务不存在或已经处理");
        }
        List<Task> visible = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateOrAssigned(username)
                .active()
                .list();
        if (visible.isEmpty()) {
            throw new BizException(403, "当前账号无权查看此任务");
        }
        return task;
    }

    private TaskView toTaskView(Task task) {
        String requestId = requestId(task);
        LeaveRequest request = findRequest(requestId);
        TaskView view = new TaskView();
        view.setTaskId(task.getId());
        view.setTaskName(task.getName());
        view.setTaskDefinitionKey(task.getTaskDefinitionKey());
        view.setTaskDefinitionKeyLabel(nodeLabel(task.getTaskDefinitionKey()));
        view.setCreateTime(task.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        view.setAssignee(task.getAssignee());
        view.setRequestId(request.getId());
        view.setApplicantName(request.getApplicantName());
        view.setDepartment(request.getDepartment());
        view.setLeaveType(request.getLeaveType());
        view.setStartTime(request.getStartTime());
        view.setEndTime(request.getEndTime());
        view.setLeaveDays(request.getLeaveDays());
        view.setReason(request.getReason());
        view.setHasMedicalProof(request.isHasMedicalProof());
        view.setStatus(request.getStatus());
        view.setCurrentNode(request.getCurrentNode());
        view.setRecords(recordViews(request.getId()));
        return view;
    }

    private List<LeaveRecordView> recordViews(String requestId) {
        List<LeaveRecord> records = leaveRecordMapper.selectList(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getRequestId, requestId)
                .orderByAsc(LeaveRecord::getRecordSeq));
        List<LeaveRecordView> views = new ArrayList<LeaveRecordView>();
        for (LeaveRecord record : records) {
            LeaveRecordView view = new LeaveRecordView();
            view.setId(record.getId());
            view.setRequestId(record.getRequestId());
            view.setNode(record.getNode());
            view.setAction(record.getAction());
            view.setOperator(record.getOperator());
            view.setComment(record.getComment());
            view.setCreatedAt(record.getCreatedAt());
            views.add(view);
        }
        return views;
    }

    private Map<String, Object> processVariables(LeaveRequest request) {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("requestId", request.getId());
        variables.put("applicant", request.getApplicant());
        variables.put("applicantName", request.getApplicantName());
        variables.put("leaveType", request.getLeaveType());
        variables.put("leaveDays", request.getLeaveDays().doubleValue());
        variables.put("hasMedicalProof", request.isHasMedicalProof());
        variables.put("needHr", needsHr(request));
        variables.put("needDirector", request.getLeaveDays().doubleValue() >= 5.0D);
        variables.put("approveManager", false);
        variables.put("returnToEmployee", false);
        variables.put("hrApproved", false);
        variables.put("directorApproved", false);
        variables.put("outcome", "");
        variables.put("lastComment", "提交请假申请");
        return variables;
    }

    private boolean needsHr(LeaveRequest request) {
        if (request.getLeaveDays().doubleValue() > 3.0D) {
            return true;
        }
        return LeaveProcessConstants.TYPE_SICK.equals(request.getLeaveType())
                || LeaveProcessConstants.TYPE_MARRIAGE.equals(request.getLeaveType())
                || LeaveProcessConstants.TYPE_BEREAVEMENT.equals(request.getLeaveType());
    }

    private void validateForm(LeaveRequestForm form) {
        if (!isLeaveType(form.getLeaveType())) {
            throw new BizException(400, "不支持的请假类型");
        }
        if (form.getEndTime().isBefore(form.getStartTime())) {
            throw new BizException(400, "结束时间不能早于开始时间");
        }
        BigDecimal halfDay = new BigDecimal("0.5");
        if (form.getLeaveDays().remainder(halfDay).signum() != 0) {
            throw new BizException(400, "请假天数必须是 0.5 的整数倍");
        }
        if (LeaveProcessConstants.TYPE_SICK.equals(form.getLeaveType())
                && form.getLeaveDays().doubleValue() > 1.0D
                && !form.isHasMedicalProof()) {
            throw new BizException(400, "病假超过 1 天需要提供医疗证明");
        }
        if (LeaveProcessConstants.TYPE_ANNUAL.equals(form.getLeaveType())) {
            LeaveBalanceView balance = balance();
            if (form.getLeaveDays().compareTo(balance.getRemainingDays()) > 0) {
                throw new BizException(400, "年假余额不足，剩余 " + balance.getRemainingDays() + " 天");
            }
        }
    }

    private boolean isLeaveType(String leaveType) {
        return LeaveProcessConstants.TYPE_ANNUAL.equals(leaveType)
                || LeaveProcessConstants.TYPE_SICK.equals(leaveType)
                || LeaveProcessConstants.TYPE_PERSONAL.equals(leaveType)
                || LeaveProcessConstants.TYPE_MARRIAGE.equals(leaveType)
                || LeaveProcessConstants.TYPE_BEREAVEMENT.equals(leaveType);
    }

    private String statusForNode(String node) {
        if (LeaveProcessConstants.NODE_RESUBMIT.equals(node)) {
            return LeaveProcessConstants.STATUS_RETURNED;
        }
        if (LeaveProcessConstants.NODE_MANAGER.equals(node)) {
            return LeaveProcessConstants.STATUS_SUBMITTED;
        }
        if (LeaveProcessConstants.NODE_HR.equals(node)) {
            return LeaveProcessConstants.STATUS_MANAGER_APPROVED;
        }
        if (LeaveProcessConstants.NODE_DIRECTOR.equals(node)) {
            return LeaveProcessConstants.STATUS_HR_APPROVED;
        }
        if (LeaveProcessConstants.NODE_CONFIRM.equals(node)) {
            return LeaveProcessConstants.STATUS_CONFIRM_PENDING;
        }
        return LeaveProcessConstants.STATUS_SUBMITTED;
    }

    private String nodeLabel(String node) {
        if (LeaveProcessConstants.NODE_MANAGER.equals(node)) {
            return "部门经理审批";
        }
        if (LeaveProcessConstants.NODE_HR.equals(node)) {
            return "HR 审批";
        }
        if (LeaveProcessConstants.NODE_DIRECTOR.equals(node)) {
            return "总监审批";
        }
        if (LeaveProcessConstants.NODE_RESUBMIT.equals(node)) {
            return "员工补充重填";
        }
        if (LeaveProcessConstants.NODE_CONFIRM.equals(node)) {
            return "员工确认安排";
        }
        return node;
    }

    private LeaveRequest findRequest(String requestId) {
        LeaveRequest request = leaveRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BizException(404, "请假申请不存在");
        }
        return request;
    }

    private SysUser loadUser(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
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

    private String requestId(Task task) {
        String requestId = (String) runtimeService.getVariable(task.getExecutionId(), "requestId");
        if (requestId == null) {
            throw new BizException(409, "流程变量中缺少 requestId，请检查 BPMN 与 Java 代码");
        }
        return requestId;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BizException(401, "请先登录");
        }
        return authentication.getName();
    }

    private boolean hasViewAllPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> RbacPermissions.LEAVE_VIEW_ALL.equals(authority.getAuthority()));
    }

}
