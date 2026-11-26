package com.example.agentscope.travel.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * MySQL 中保存的旅游知识片段。
 */
@Entity
@Table(name = "travel_knowledge_chunk")
public class TravelKnowledgeChunk {

    /**
     * 由来源文件名和片段序号组成的稳定编号。
     */
    @Id
    @Column(nullable = false, length = 128)
    private String id;

    /**
     * 文档标题与段落标题的组合。
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 知识来源文件名。
     */
    @Column(nullable = false, length = 255)
    private String source;

    /**
     * 知识正文，使用 TEXT 列保存较长段落。
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 首次写入数据库的时间。
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 最近一次内容更新的时间。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TravelKnowledgeChunk() {
    }

    public TravelKnowledgeChunk(
            String id,
            String title,
            String source,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
