package com.example.redission.dao;

import com.example.redission.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageDao extends JpaRepository<Storage,Integer>, JpaSpecificationExecutor<Storage> {

    @Modifying
    @Query(value = " UPDATE t_storage SET number = number-1 WHERE `id` =  ?1 ",nativeQuery = true)
    Integer reduceStorageNumber(Integer produceId);
}
