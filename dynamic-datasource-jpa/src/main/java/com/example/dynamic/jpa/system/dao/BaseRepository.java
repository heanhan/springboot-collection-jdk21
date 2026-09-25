package com.example.dynamic.jpa.system.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@org.springframework.data.repository.NoRepositoryBean
public interface BaseRepository<T,D> extends JpaSpecificationExecutor<T>, JpaRepository<T,D> {
}
