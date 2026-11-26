package com.example.agentscope.travel.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 旅游问答接口的响应结果。
 */
public class TravelChatResponse {

    /**
     * 本次请求使用的会话编号。
     */
    private String sessionId;

    /**
     * 路由器最终选择的专家类型。
     */
    private AgentType routedAgent;

    /**
     * 路由器给出的路由理由。
     */
    private String routeReason;

    /**
     * 整理后的旅游指南内容。
     */
    private String answer;

    /**
     * 提醒游客通过官方渠道核实实时信息。
     */
    private String notice;

    /**
     * 本次检索并提供给 Agent 的知识片段，顺序与答案中的引用编号一致。
     */
    private List<KnowledgeSearchResult> knowledgeSources = new ArrayList<KnowledgeSearchResult>();

    public TravelChatResponse() {
    }

    public TravelChatResponse(
            String sessionId,
            AgentType routedAgent,
            String routeReason,
            String answer,
            String notice,
            List<KnowledgeSearchResult> knowledgeSources) {
        this.sessionId = sessionId;
        this.routedAgent = routedAgent;
        this.routeReason = routeReason;
        this.answer = answer;
        this.notice = notice;
        this.knowledgeSources = knowledgeSources;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AgentType getRoutedAgent() {
        return routedAgent;
    }

    public void setRoutedAgent(AgentType routedAgent) {
        this.routedAgent = routedAgent;
    }

    public String getRouteReason() {
        return routeReason;
    }

    public void setRouteReason(String routeReason) {
        this.routeReason = routeReason;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public List<KnowledgeSearchResult> getKnowledgeSources() {
        return knowledgeSources;
    }

    public void setKnowledgeSources(List<KnowledgeSearchResult> knowledgeSources) {
        this.knowledgeSources = knowledgeSources;
    }
}
