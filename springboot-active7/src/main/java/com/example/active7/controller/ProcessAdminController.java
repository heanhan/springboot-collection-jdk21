package com.example.active7.controller;

import com.example.common.exceptins.BizException;
import com.example.common.result.ResultBody;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.activiti.engine.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 管理员流程观察接口，对应 RepositoryService 的查询能力。
 */
@RestController
@RequestMapping("/api/admin")
public class ProcessAdminController {

    private final RepositoryService repositoryService;
    private final TaskService taskService;

    public ProcessAdminController(RepositoryService repositoryService, TaskService taskService) {
        this.repositoryService = repositoryService;
        this.taskService = taskService;
    }

    @GetMapping("/deployments")
    public ResultBody<List<Map<String, Object>>> deployments() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", deployment.getId());
            item.put("name", deployment.getName());
            item.put("category", deployment.getCategory());
            item.put("deploymentTime", deployment.getDeploymentTime());
            result.add(item);
        }
        return ResultBody.success(result);
    }

    @GetMapping("/definitions")
    public ResultBody<List<Map<String, Object>>> definitions() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ProcessDefinition definition : repositoryService.createProcessDefinitionQuery().list()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", definition.getId());
            item.put("name", definition.getName());
            item.put("key", definition.getKey());
            item.put("version", definition.getVersion());
            item.put("deploymentId", definition.getDeploymentId());
            item.put("resourceName", definition.getResourceName());
            item.put("suspended", definition.isSuspended());
            result.add(item);
        }
        return ResultBody.success(result);
    }

    @GetMapping("/tasks")
    public ResultBody<List<Map<String, Object>>> allTasks() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Task task : taskService.createTaskQuery().active().list()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("assignee", task.getAssignee());
            item.put("processInstanceId", task.getProcessInstanceId());
            result.add(item);
        }
        return ResultBody.success(result);
    }

    @GetMapping("/process-xml/{definitionId}")
    public ResultBody<String> processXml(@PathVariable String definitionId) throws IOException {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (definition == null) {
            throw new BizException(404, "流程定义不存在");
        }
        String resourceName = definition.getResourceName();
        try (InputStream inputStream = repositoryService.getResourceAsStream(
                definition.getDeploymentId(), resourceName);
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return ResultBody.success(scanner.useDelimiter("\\A").next());
        }
    }
}
