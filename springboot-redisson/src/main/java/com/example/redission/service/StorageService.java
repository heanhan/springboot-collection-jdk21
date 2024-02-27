package com.example.redission.service;

import com.example.redission.entity.Storage;

public interface StorageService {

    /**
     * 根据产品id 查询产品
     * @param id
     * @return
     */
    Storage findStorageById(Integer id);

    /**
     * 减库存
     * @param produceId
     * @return
     */
    Integer reduceStorageNumber(Integer produceId);
}
