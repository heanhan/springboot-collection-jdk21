package com.example.ai.langchain4j.config;

import com.example.ai.langchain4j.properties.OllamaConfigProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 19:46
 * @Description:
 */
@Configuration
public class OllamaChatModelConfig {

    @Resource
    private OllamaConfigProperties ollamaConfigProperties;

    @Bean
    public ChatLanguageModel OllamaModel() {
        return OllamaChatModel.builder()
                .modelName(ollamaConfigProperties.getModelName())
                .baseUrl(ollamaConfigProperties.getUrl())
                .build();
    }
}
