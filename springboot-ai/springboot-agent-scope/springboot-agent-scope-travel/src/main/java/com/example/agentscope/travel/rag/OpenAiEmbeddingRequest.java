package com.example.agentscope.travel.rag;

/**
 * OpenAI 兼容 Embedding 接口请求体。
 */
final class OpenAiEmbeddingRequest {

    private final String model;
    private final String input;
    private final int dimensions;

    OpenAiEmbeddingRequest(String model, String input, int dimensions) {
        this.model = model;
        this.input = input;
        this.dimensions = dimensions;
    }

    public String getModel() {
        return model;
    }

    public String getInput() {
        return input;
    }

    public int getDimensions() {
        return dimensions;
    }
}
