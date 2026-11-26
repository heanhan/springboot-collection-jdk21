package com.example.agentscope.travel.rag;

/**
 * Markdown 解析阶段使用的旅游知识片段，只保存生成数据库实体所需的内容。
 */
public class TravelKnowledgeDocument {

    private final String id;
    private final String title;
    private final String source;
    private final String content;

    public TravelKnowledgeDocument(String id, String title, String source, String content) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }
}
