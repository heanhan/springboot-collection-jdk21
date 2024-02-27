package com.example.redission.service.impl;

import com.example.redission.dao.StorageDao;
import com.example.redission.entity.Storage;
import com.example.redission.service.StorageService;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    @Resource
    private StorageDao storageDao;
    /**
     * 根据产品id 查询产品
     *
     * @param id
     * @return
     */
    @Override
    public Storage findStorageById(Integer id) {
        Optional<Storage> byId = storageDao.findById(id);
        return byId.orElse(null);
    }

    /**
     * 减库存
     *
     * @param produceId
     * @return
     */
    @Override
    @Transactional
    public Integer reduceStorageNumber(Integer produceId) {
        Integer flag = storageDao.reduceStorageNumber(produceId);
        return flag;
    }
}
