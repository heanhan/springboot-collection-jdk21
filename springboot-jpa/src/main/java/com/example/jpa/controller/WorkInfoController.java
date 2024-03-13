package com.example.jpa.controller;


import com.example.common.enums.CommonEnum;
import com.example.common.result.ResultBody;
import com.example.jpa.entity.WorkInfo;
import com.example.jpa.service.WorkInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/workInfo")
public class WorkInfoController {

    @Resource
    private WorkInfoService workInfoService;

    /**
     * 添加用户的工作经历
     * @param workInfo
     * @return
     */
    @PostMapping(value = "/addWorkInfo")
    public ResultBody addWorkInfo(@RequestBody WorkInfo workInfo) {
        try {
            WorkInfo info = workInfoService.addWorkInfo(workInfo);
        } catch (Exception e) {
            log.info("工作经历添加失败,报错信息：{}", e.getMessage());
            return ResultBody.error(CommonEnum.ERROR);
        }
        return ResultBody.success();

    }


}
