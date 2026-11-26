package com.example.agentscope.travel.rag;

/**
 * 知识向量生成接口，后续部署真实 Embedding 模型时替换当前实现即可。
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
     * 把文本转换成固定维度的浮点向量。
     */
    float[] embed(String text);
}
