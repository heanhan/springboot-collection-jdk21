package com.example.dynamic.jpa.system.dao;

import com.example.dynamic.jpa.system.entity.ProvisioningTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvisioningTaskDao extends JpaRepository<ProvisioningTask, Integer> {

    List<ProvisioningTask> findTop10ByTenantIdOrderByIdDesc(Integer tenantId);
}
