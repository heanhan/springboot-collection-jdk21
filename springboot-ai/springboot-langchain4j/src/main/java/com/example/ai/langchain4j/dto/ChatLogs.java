package com.example.ai.langchain4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 9:39
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatLogs implements Serializable {

    private Integer id; // 主键，唯一标识每条记录

    private Integer userId; // 用户ID，用于关联用户表

    private String sessionId; // 会话ID，用于标识同一会话中的记录

    private String modelName; // GPT模型的名称或版本

    private String chatContent; // 聊天内容，存储实际的对话文本

    private Timestamp timestamp; // 时间戳，记录聊天内容生成的时间

    private String context; // 上下文信息，存储聊天的上下文信息

}
