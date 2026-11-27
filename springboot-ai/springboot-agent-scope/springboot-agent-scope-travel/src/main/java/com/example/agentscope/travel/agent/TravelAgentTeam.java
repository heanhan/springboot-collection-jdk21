package com.example.agentscope.travel.agent;

import com.example.agentscope.travel.api.AgentType;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.Map;

/**
 * 保存旅游场景使用的路由 Agent、专家 Agent 和指南 Agent。
 */
public class TravelAgentTeam implements AutoCloseable {

    /**
     * 负责判断用户问题应该交给哪位专家。
     */
    private final HarnessAgent router;

    /**
     * 专家类型与对应 Agent 的映射。
     */
    private final Map<AgentType, HarnessAgent> specialists;

    /**
     * 负责把专家材料整理成最终旅游指南。
     */
    private final HarnessAgent guide;

    public TravelAgentTeam(
            HarnessAgent router,
            Map<AgentType, HarnessAgent> specialists,
            HarnessAgent guide) {
        this.router = router;
        this.specialists = specialists;
        this.guide = guide;
    }

    public HarnessAgent getRouter() {
        return router;
    }

    public Map<AgentType, HarnessAgent> getSpecialists() {
        return specialists;
    }

    public HarnessAgent getGuide() {
        return guide;
    }

    /**
     * Spring 销毁 Bean 时关闭所有 Agent，等待 Guide 后台记忆任务完成。
     */
    @Override
    public void close() {
        guide.close();
        router.close();
        for (HarnessAgent specialist : specialists.values()) {
            specialist.close();
        }
    }
}
