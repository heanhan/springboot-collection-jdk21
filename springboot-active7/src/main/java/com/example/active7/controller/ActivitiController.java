package com.example.active7.controller;

import com.example.common.result.ResultBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import com.example.active7.security.RbacPermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Active7 动态部署示例：适合学习如何把 bpmn/bar 压缩包部署到引擎。
 */
@Tag(name = "Active7 流程部署", description = "管理员上传 bpmn/bar 压缩包")
@RestController
@RequestMapping("/api/admin")
public class ActivitiController {

    private final RepositoryService repositoryService;

    public ActivitiController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PreAuthorize("hasAuthority('" + RbacPermissions.PROCESS_DEPLOY + "')")
    @Operation(summary = "上传 BPMN ZIP 并动态部署")
    @PostMapping("/deploy/zip")
    public ResultBody<Map<String, Object>> deploy(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("部署文件不能为空");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            Deployment deployment = repositoryService.createDeployment()
                    .name(file.getOriginalFilename())
                    .addZipInputStream(zipInputStream)
                    .deploy();
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("deploymentId", deployment.getId());
            result.put("name", deployment.getName());
            result.put("deploymentTime", deployment.getDeploymentTime());
            return ResultBody.success(result);
        }
    }
}
