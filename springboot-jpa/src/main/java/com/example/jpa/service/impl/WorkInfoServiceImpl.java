package com.example.jpa.service.impl;

import com.example.jpa.entity.WorkInfo;
import com.example.jpa.repository.WorkInfoRepository;
import com.example.jpa.service.WorkInfoService;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WorkInfoServiceImpl implements WorkInfoService {

    @Resource
    private WorkInfoRepository workInfoRepository;

    /**
     * 添加用户的经历
     *
     * @param workInfo
     * @return
     */
    @Transactional
    @Override
    public WorkInfo addWorkInfo(WorkInfo workInfo) {
        WorkInfo save = workInfoRepository.save(workInfo);
        return save;
    }
}
