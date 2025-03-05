package com.example.ai.langchain4j.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.example.ai.langchain4j.config.RabbitMQConfig;
import com.example.ai.langchain4j.constant.RedisConstants;
import com.example.ai.langchain4j.dto.ChatLogs;
import com.example.ai.langchain4j.mapper.ChatLogsMapper;
import com.example.ai.langchain4j.properties.OllamaConfigProperties;
import com.example.ai.langchain4j.service.ChatLogsService;
import com.example.ai.langchain4j.service.IOllamaChatModelService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 10:04
 * @Description:
 */
@Service
public class OllamaChatModelServiceImpl implements IOllamaChatModelService {
    @Resource
    private ChatLanguageModel chatLanguageModel;

    @Resource
    private RedisTemplate redisTemplate;
    
    @Resource
    private ChatLogsService chatLogsService;

    @Resource
    private OllamaConfigProperties ollamaConfigProperties;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public String chat(String message) {
        //1、调用大模型的接口
        String generate = chatLanguageModel.generate(message);
        //2、以下是进行业务数据的处理
        //实际生产中 需要从登录中获取用户id信息、通过生成一个策略的会话id
        ChatLogs chatLog = ChatLogs.builder()
                .userId(1)
                .chatContent(generate)
                .modelName(ollamaConfigProperties.getModelName())
                .sessionId("123")
                .build();

        String jsonString = JSONObject.toJSONString(chatLog);
        String key = RedisConstants.CacheName.CHAT_CACHE + "::" + chatLog.getUserId();
        redisTemplate.opsForList().rightPush(key, jsonString);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,RabbitMQConfig.ROUTING_KEY,jsonString);
        chatLogsService.save(chatLog);
        return generate;
    }
}
