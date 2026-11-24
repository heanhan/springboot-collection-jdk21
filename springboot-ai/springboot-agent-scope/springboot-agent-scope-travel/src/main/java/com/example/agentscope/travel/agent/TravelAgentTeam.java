package com.example.agentscope.travel.agent;

import com.example.agentscope.travel.api.AgentType;
import io.agentscope.core.ReActAgent;

import java.util.Map;

/**
 * 保存旅游场景使用的路由 Agent、专家 Agent 和指南 Agent。
 */
public class TravelAgentTeam {

    /**
     * 负责判断用户问题应该交给哪位专家。
     */
    private final ReActAgent router;

    /**
     * 专家类型与对应 Agent 的映射。
     */
    private final Map<AgentType, ReActAgent> specialists;

    /**
     * 负责把专家材料整理成最终旅游指南。
     */
    private final ReActAgent guide;

    public TravelAgentTeam(
            ReActAgent router,
            Map<AgentType, ReActAgent> specialists,
            ReActAgent guide) {
        this.router = router;
        this.specialists = specialists;
        this.guide = guide;
    }

    public ReActAgent getRouter() {
        return router;
    }

    public Map<AgentType, ReActAgent> getSpecialists() {
        return specialists;
    }

    public ReActAgent getGuide() {
        return guide;
    }
}
