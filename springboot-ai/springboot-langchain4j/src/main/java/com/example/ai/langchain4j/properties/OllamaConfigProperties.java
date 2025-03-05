package com.example.ai.langchain4j.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 19:54
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaConfigProperties implements Serializable {
    private String url;
    private String modelName;
    private String dockerImageName;
    private Integer port;
}
