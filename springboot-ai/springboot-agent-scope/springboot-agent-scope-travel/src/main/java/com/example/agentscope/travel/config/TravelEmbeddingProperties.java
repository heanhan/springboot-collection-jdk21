package com.example.agentscope.travel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 本地 OpenAI 兼容 Embedding 服务配置。
 */
@Validated
@ConfigurationProperties(prefix = "travel.rag.embedding")
public class TravelEmbeddingProperties {

    /**
     * Embedding 服务地址，通常以 /v1 结尾。
     */
    @NotBlank(message = "Embedding 服务地址不能为空")
    private String baseUrl = "http://127.0.0.1:8000/v1";

    /**
     * 本地服务密钥；为空时不发送 Authorization 请求头。
     */
    private String apiKey;

    /**
     * Embedding 服务注册的模型标识。
     */
    @NotBlank(message = "Embedding 模型名称不能为空")
    @Size(max = 100, message = "Embedding 模型名称最长不能超过 100 个字符")
    private String modelName = "Qwen3-Embedding-0.6B";

    /**
     * 文档和查询向量使用的统一维度。
     */
    @Min(value = 32, message = "Embedding 维度最少为 32")
    @Max(value = 1024, message = "Embedding 维度最多为 1024")
    private int dimension = 1024;

    /**
     * 调用 Embedding 服务的最长秒数。
     */
    @Min(value = 1, message = "Embedding 超时时间最少为 1 秒")
    @Max(value = 600, message = "Embedding 超时时间最多为 600 秒")
    private int timeoutSeconds = 60;

    /**
     * 仅添加到查询侧的 Qwen3 检索指令。
     */
    @NotBlank(message = "Embedding 查询指令不能为空")
    @Size(max = 500, message = "Embedding 查询指令最长不能超过 500 个字符")
    private String queryInstruction =
            "Instruct: Given a web search query, retrieve relevant passages that answer the query";

    public TravelEmbeddingProperties() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getQueryInstruction() {
        return queryInstruction;
    }

    public void setQueryInstruction(String queryInstruction) {
        this.queryInstruction = queryInstruction;
    }
}
