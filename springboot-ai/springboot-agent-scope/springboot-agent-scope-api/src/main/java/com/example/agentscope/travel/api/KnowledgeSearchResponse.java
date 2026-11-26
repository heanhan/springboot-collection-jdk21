package com.example.agentscope.travel.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 旅游知识检索响应。
 */
public class KnowledgeSearchResponse {

    /**
     * 原始检索问题。
     */
    private String query;

    /**
     * 本次实际使用的 Top-K 数量。
     */
    private int topK;

    /**
     * 达到向量相似度阈值并返回的知识片段数量。
     */
    private int totalMatches;

    /**
     * 按匹配分数从高到低排列的知识片段。
     */
    private List<KnowledgeSearchResult> results = new ArrayList<KnowledgeSearchResult>();

    public KnowledgeSearchResponse() {
    }

    public KnowledgeSearchResponse(String query, int topK, List<KnowledgeSearchResult> results) {
        this.query = query;
        this.topK = topK;
        this.results = results;
        this.totalMatches = results.size();
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

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public List<KnowledgeSearchResult> getResults() {
        return results;
    }

    public void setResults(List<KnowledgeSearchResult> results) {
        this.results = results;
        this.totalMatches = results.size();
    }
}
