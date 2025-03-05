package com.example.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ai.langchain4j.dto.ChatLogs;
import com.example.ai.langchain4j.mapper.ChatLogsMapper;
import com.example.ai.langchain4j.service.ChatLogsService;
import org.springframework.stereotype.Service;

/**
 * @author zhaojh
 * @version 1.0
 * @description TODO
 * @date 2025/3/5 下午2:27
 */

@Service
public class ChatLogsServiceImpl extends ServiceImpl<ChatLogsMapper,ChatLogs> implements ChatLogsService {
}
