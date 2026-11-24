package com.example.agentscope.travel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 旅游 Agent 的模型、超时和状态存储配置。
 */
@Validated
@ConfigurationProperties(prefix = "travel.agent")
public class TravelAgentProperties {

    /**
     * 本地 OpenAI 兼容服务的访问密钥。
     */
    @NotBlank(message = "请通过 OPENAI_API_KEY 配置本地模型密钥")
    private String apiKey;

    /**
     * 本地模型服务的 API 地址。
     */
    private String baseUrl = "http://127.0.0.1:8000/v1";

    /**
     * 本地模型服务提供的模型名称。
     */
    private String modelName = "Qwen3.5-4B-MLX-4bit";

    /**
     * 是否使用流式方式生成模型内容。
     */
    private boolean stream = true;

    /**
     * 模型生成内容的随机程度。
     */
    @Min(0)
    @Max(2)
    private double temperature = 0.2;

    /**
     * 单次模型回答允许生成的最大 Token 数量。
     */
    @Min(256)
    private int maxTokens = 2048;

    /**
     * 等待单次 Agent 调用的最长秒数。
     */
    @Min(10)
    private long timeoutSeconds = 180;

    /**
     * Agent 会话状态的本地保存目录。
     */
    private Path stateDirectory = Paths.get(".agentscope-data/travel");

    public TravelAgentProperties() {
    }

    public TravelAgentProperties(
            String apiKey,
            String baseUrl,
            String modelName,
            boolean stream,
            double temperature,
            int maxTokens,
            long timeoutSeconds,
            Path stateDirectory) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.stream = stream;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.stateDirectory = stateDirectory;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Path getStateDirectory() {
        return stateDirectory;
    }

    public void setStateDirectory(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
    }
}
