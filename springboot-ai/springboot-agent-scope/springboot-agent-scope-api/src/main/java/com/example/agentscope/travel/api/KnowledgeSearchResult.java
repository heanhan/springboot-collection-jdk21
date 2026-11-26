package com.example.agentscope.travel.api;

/**
 * 单条命中的旅游知识片段。
 */
public class KnowledgeSearchResult {

    /**
     * 知识片段编号。
     */
    private String id;

    /**
     * 知识片段标题。
     */
    private String title;

    /**
     * 知识来源文件名。
     */
    private String source;

    /**
     * 向量相似度百分比，范围 0 到 100，越高越相似。
     */
    private int score;

    /**
     * 检索到的知识正文。
     */
    private String content;

    public KnowledgeSearchResult() {
    }

    public KnowledgeSearchResult(String id, String title, String source, int score, String content) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.score = score;
        this.content = content;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
