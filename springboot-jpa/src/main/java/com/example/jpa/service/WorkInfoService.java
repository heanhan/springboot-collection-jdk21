package com.example.jpa.service;

import com.example.jpa.entity.WorkInfo;

public interface WorkInfoService {

    /**
     * 添加用户的经历
     * @param workInfo
     * @return
     */
    WorkInfo addWorkInfo(WorkInfo workInfo);
}
