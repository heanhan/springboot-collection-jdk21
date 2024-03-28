package com.example.sharding.dao;

import com.example.sharding.entity.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelDao extends JpaRepository<Travel,Long>, JpaSpecificationExecutor<Travel> {
}
