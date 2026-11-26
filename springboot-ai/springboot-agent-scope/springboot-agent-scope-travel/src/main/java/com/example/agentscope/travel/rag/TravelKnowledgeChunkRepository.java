package com.example.agentscope.travel.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 旅游知识片段的 MySQL 持久化入口。
 */
public interface TravelKnowledgeChunkRepository extends JpaRepository<TravelKnowledgeChunk, String> {

    List<TravelKnowledgeChunk> findAllByOrderByIdAsc();
}
