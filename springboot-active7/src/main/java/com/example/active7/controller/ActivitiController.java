package com.example.active7.controller;

import com.example.common.result.ResultBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * @Description 启动类
 * @Author : zhaojh
 * @Date : 2024/7/4 15:24
 */

@Slf4j
@Tag(name = "Activiti7流程管理-模块")
@RestController
public class ActivitiController {

//    @Resource
//    private RepositoryService repositoryService;
//
//    @PostMapping(value = "/deploy")
//    public ResultBody<Object> deploy(@RequestPart("file") MultipartFile file) {
//        try {
//            if (file.isEmpty()) {
//                throw new NullPointerException("部署压缩包不能为空");
//            }
//            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
//            //压缩流
//            ZipInputStream zip = new ZipInputStream(file.getInputStream());
//            deploymentBuilder.addZipInputStream(zip);
//            //设置部署流程名称
//            deploymentBuilder.name("请假审批");
//            //部署流程
//            Deployment deploy = deploymentBuilder.deploy();
//            return ResultBody.success(deploy);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResultBody.error(e.toString());
//        }
//    }
//
//    @PostMapping("/queryDeploymentInfo")
//    public ResultBody<Object> queryDeploymentInfo() {
//        //也可以设置查询部署筛选条件，自行查询API，基本上都是见名知意的
//        List<Deployment> list = repositoryService.createDeploymentQuery().list();
//        log.info("流程部署信息：{}", list);
//        return ResultBody.success(list.toString());
//    }


}
