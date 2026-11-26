package com.example.agentscope.travel.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 旅游知识检索请求。
 */
public class KnowledgeSearchRequest {

    /**
     * 需要检索的旅游问题。
     */
    @NotBlank(message = "query 不能为空")
    @Size(max = 500, message = "query 最长不能超过 500 个字符")
    private String query;

    /**
     * 本次请求返回的最大片段数量，未传时使用系统默认值。
     */
    @Min(value = 1, message = "topK 最小为 1")
    @Max(value = 10, message = "topK 最大为 10")
    private int topK = 3;

    public KnowledgeSearchRequest() {
    }

    public KnowledgeSearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
