package com.example.agentscope.travel.rag;

/**
 * 知识向量生成接口，文档与查询可以使用不同的输入规则。
 */
public interface TravelEmbeddingService {

    /**
     * 返回向量维度，知识向量和查询向量必须使用相同维度。
     */
    int getDimension();

    /**
     * 返回向量模型标记，避免不同模型生成的向量混在同一张表中。
     */
    String getModelName();

    /**
     * 把知识文档转换成固定维度的浮点向量。
     */
    float[] embedDocument(String text);

    /**
     * 把游客问题转换成查询向量，可添加检索指令。
     */
    float[] embedQuery(String text);
}
