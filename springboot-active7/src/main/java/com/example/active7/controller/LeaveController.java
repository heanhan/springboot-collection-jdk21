package com.example.active7.controller;

import com.example.active7.service.LeaveService;
import com.example.active7.service.dto.ApprovalForm;
import com.example.active7.service.dto.LeaveBalanceView;
import com.example.active7.service.dto.LeaveRecordView;
import com.example.active7.service.dto.LeaveRequestForm;
import com.example.active7.service.dto.TaskView;
import com.example.active7.model.LeaveRequest;
import com.example.common.result.ResultBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/requests")
    public ResultBody<String> submit(@Valid @RequestBody LeaveRequestForm form) {
        return ResultBody.success(leaveService.submit(form));
    }

    @GetMapping("/requests")
    public ResultBody<List<LeaveRequest>> allRequests() {
        return ResultBody.success(leaveService.listAll());
    }

    @GetMapping("/requests/mine")
    public ResultBody<List<LeaveRequest>> myRequests() {
        return ResultBody.success(leaveService.listMine());
    }

    @GetMapping("/requests/{requestId}")
    public ResultBody<LeaveRequest> requestDetail(@PathVariable String requestId) {
        return ResultBody.success(leaveService.getMine(requestId));
    }

    @GetMapping("/requests/{requestId}/records")
    public ResultBody<List<LeaveRecordView>> records(@PathVariable String requestId) {
        return ResultBody.success(leaveService.records(requestId));
    }

    @PostMapping("/requests/{requestId}/withdraw")
    public ResultBody<Void> withdraw(@PathVariable String requestId) {
        leaveService.withdraw(requestId);
        return ResultBody.success();
    }

    @GetMapping("/balance")
    public ResultBody<LeaveBalanceView> balance() {
        return ResultBody.success(leaveService.balance());
    }

    @GetMapping("/tasks/mine")
    public ResultBody<List<TaskView>> myTasks() {
        return ResultBody.success(leaveService.listMyTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ResultBody<TaskView> taskDetail(@PathVariable String taskId) {
        return ResultBody.success(leaveService.taskDetail(taskId));
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ResultBody<Void> claim(@PathVariable String taskId) {
        leaveService.claim(taskId);
        return ResultBody.success();
    }

    @PostMapping("/tasks/{taskId}/approve")
    public ResultBody<Void> approve(@PathVariable String taskId,
                                    @Valid @RequestBody ApprovalForm form) {
        leaveService.approve(taskId, form);
        return ResultBody.success();
    }

    @PostMapping("/tasks/{taskId}/resubmit")
    public ResultBody<Void> resubmit(@PathVariable String taskId,
                                     @Valid @RequestBody LeaveRequestForm form) {
        leaveService.resubmit(taskId, form);
        return ResultBody.success();
    }
}
