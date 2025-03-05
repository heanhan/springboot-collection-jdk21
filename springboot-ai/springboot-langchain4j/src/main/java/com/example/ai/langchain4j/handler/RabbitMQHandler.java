package com.example.ai.langchain4j.handler;

import com.alibaba.fastjson2.JSONObject;
import com.example.ai.langchain4j.config.RabbitMQConfig;
import com.example.ai.langchain4j.dto.ChatLogs;
import com.example.ai.langchain4j.mapper.ChatLogsMapper;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 14:47
 * @Description:
 */
@Component
public class RabbitMQHandler {

    @Resource
    private ChatLogsMapper messageMapper;

    @RabbitListener(queues = {RabbitMQConfig.QUEUE_NAME})
    public void saveMessage(String chatLogsJson){
        ChatLogs chatLog = JSONObject.parseObject(chatLogsJson, ChatLogs.class);
        System.out.print(chatLogsJson);
       messageMapper.insert(chatLog);
    }
}
