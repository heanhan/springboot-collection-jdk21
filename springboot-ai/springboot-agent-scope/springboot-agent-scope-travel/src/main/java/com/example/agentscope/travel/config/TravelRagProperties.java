package com.example.agentscope.travel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 旅游知识库检索配置。
 */
@Validated
@ConfigurationProperties(prefix = "travel.rag")
public class TravelRagProperties {

    /**
     * 知识文件的 classpath 匹配规则。
     */
    @NotBlank(message = "知识文件匹配规则不能为空")
    private String knowledgePattern = "classpath*:knowledge/travel/*.md";

    /**
     * 默认返回的最大知识片段数量。
     */
    @Min(1)
    @Max(10)
    private int topK = 3;

    public TravelRagProperties() {
    }

    public TravelRagProperties(String knowledgePattern, int topK) {
        this.knowledgePattern = knowledgePattern;
        this.topK = topK;
    }

    public String getKnowledgePattern() {
        return knowledgePattern;
    }

    public void setKnowledgePattern(String knowledgePattern) {
        this.knowledgePattern = knowledgePattern;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
