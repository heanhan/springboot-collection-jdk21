package com.example.agentscope.travel.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * PostgreSQL pgvector 连接和检索配置。
 */
@Validated
@ConfigurationProperties(prefix = "travel.rag.vector")
public class TravelVectorProperties {

    @NotBlank(message = "PostgreSQL 连接地址不能为空")
    private String url = "jdbc:postgresql://172.16.75.106:5432/postgres";

    @NotBlank(message = "PostgreSQL 用户名不能为空")
    private String username = "postgres";

    @NotBlank(message = "请通过 PG_PASSWORD 配置 PostgreSQL 密码")
    private String password;

    @Min(0)
    @Max(1)
    private double minSimilarity = 0.15D;

    @Min(1)
    @Max(100)
    private int candidateLimit = 20;

    public TravelVectorProperties() {
    }

    public TravelVectorProperties(
            String url,
            String username,
            String password,
            double minSimilarity,
            int candidateLimit) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.minSimilarity = minSimilarity;
        this.candidateLimit = candidateLimit;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }
}
