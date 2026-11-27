package com.example.agentscope.travel.rag;

import java.util.List;

/**
 * OpenAI 兼容 Embedding 接口响应体。
 */
class OpenAiEmbeddingResponse {

    private List<EmbeddingData> data;

    public OpenAiEmbeddingResponse() {
    }

    public List<EmbeddingData> getData() {
        return data;
    }

    public void setData(List<EmbeddingData> data) {
        this.data = data;
    }

    /**
     * 单条文本的向量结果。
     */
    public static final class EmbeddingData {

        private int index;
        private List<Float> embedding;

        public EmbeddingData() {
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public List<Float> getEmbedding() {
            return embedding;
        }

        public void setEmbedding(List<Float> embedding) {
            this.embedding = embedding;
        }
    }
}
