package com.example.jpa.repository;

import com.example.jpa.entity.WorkInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkInfoRepository extends JpaRepository<WorkInfo,Long>, JpaSpecificationExecutor<WorkInfo> {
}
