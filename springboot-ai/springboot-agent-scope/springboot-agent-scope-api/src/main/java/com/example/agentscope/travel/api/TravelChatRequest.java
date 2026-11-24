package com.example.agentscope.travel.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 旅游问答接口的请求参数。
 */
public class TravelChatRequest {

    /**
     * 会话编号，用于保存和区分不同游客的对话上下文。
     */
    @NotBlank(message = "sessionId 不能为空")
    @Size(max = 100, message = "sessionId 最长不能超过 100 个字符")
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "sessionId 只能包含字母、数字、下划线和短横线")
    private String sessionId;

    /**
     * 游客本次发送的问题。
     */
    @NotBlank(message = "message 不能为空")
    @Size(max = 2000, message = "message 最长不能超过 2000 个字符")
    private String message;

    /**
     * 供 JSON 反序列化使用。
     */
    public TravelChatRequest() {
    }

    public TravelChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
