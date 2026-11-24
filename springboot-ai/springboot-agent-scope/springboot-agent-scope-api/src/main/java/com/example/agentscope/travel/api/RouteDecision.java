package com.example.agentscope.travel.api;

/**
 * 路由器 Agent 返回的结构化路由结果。
 */
public class RouteDecision {

    /**
     * 需要接洽的专家类型。
     */
    private AgentType targetAgent;

    /**
     * 路由判断的中文说明。
     */
    private String reason;

    public RouteDecision() {
    }

    public RouteDecision(AgentType targetAgent, String reason) {
        this.targetAgent = targetAgent;
        this.reason = reason;
    }

    public AgentType getTargetAgent() {
        return targetAgent;
    }

    public void setTargetAgent(AgentType targetAgent) {
        this.targetAgent = targetAgent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
