package com.example.agentscope.travel.service;

import com.example.agentscope.travel.agent.TravelAgentTeam;
import com.example.agentscope.travel.api.AgentType;
import com.example.agentscope.travel.api.RouteDecision;
import com.example.agentscope.travel.api.TravelChatRequest;
import com.example.agentscope.travel.api.TravelChatResponse;
import com.example.agentscope.travel.config.TravelAgentProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
public class TravelGuideService {

    private static final Logger log = LoggerFactory.getLogger(TravelGuideService.class);
    private static final String USER_ID = "local-demo-user";
    private static final String NOTICE =
            "本指南由本地模型生成，未实时查询交通、票价、天气或营业信息，出行前请通过官方渠道核实。";

    private final TravelAgentTeam agents;
    private final Duration timeout;

    public TravelGuideService(TravelAgentTeam agents, TravelAgentProperties properties) {
        this.agents = agents;
        this.timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
    }

    public TravelChatResponse chat(TravelChatRequest request) {
        String sessionId = request.getSessionId();
        RuntimeContext routerContext = context("travel-router-" + sessionId);
        Msg routeMessage = agents.getRouter()
                .call(request.getMessage(), RouteDecision.class, routerContext)
                .block(timeout);

        RouteDecision decision = readRouteDecision(routeMessage);
        AgentType target = Optional.ofNullable(decision.getTargetAgent()).orElse(AgentType.GENERAL);
        String reason = Optional.ofNullable(decision.getReason())
                .filter(value -> !value.isBlank())
                .orElse(defaultReason(target));

        ReActAgent specialist = agents.getSpecialists().get(target);
        String specialistInput = """
                当前日期：%s
                用户当前问题：%s
                客服保留的上下文：%s
                硬性要求：保持用户明确给出的目的地、天数和人数；未知信息必须写成假设，不得自行改写。当前日期不代表可以获取实时天气、价格或政策。
                """.formatted(LocalDate.now(), request.getMessage(), reason);
        String specialistAnswer = specialist.call(
                        specialistInput,
                        context(specialistSession(sessionId, target)))
                .block(timeout)
                .getTextContent();

        if (specialistAnswer == null || specialistAnswer.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "旅游专家暂时无法生成有效回复，请稍后重试。");
        }

        String guideInput = """
                当前日期：%s
                用户当前问题：%s
                客服保留的上下文：%s
                本次接洽专家：%s
                专家材料：
                %s
                硬性要求：保持用户明确给出的目的地、天数和人数；不得复制专家材料中与用户条件冲突的数字。
                """.formatted(
                LocalDate.now(), request.getMessage(), reason, target, specialistAnswer);
        Msg guideMessage = agents.getGuide()
                .call(guideInput, context("travel-guide-" + sessionId))
                .block(timeout);
        String answer = guideMessage.getTextContent();
        if (answer == null || answer.isBlank()) {
            log.warn("Guide agent returned an empty response for session {}", sessionId);
            answer = specialistAnswer;
        }

        return new TravelChatResponse(sessionId, target, reason, answer, NOTICE);
    }

    private RouteDecision readRouteDecision(Msg message) {
        if (message != null && message.hasStructuredData()) {
            RouteDecision decision = message.getStructuredData(RouteDecision.class);
            if (decision != null && decision.getTargetAgent() != null) {
                return decision;
            }
        }
        log.warn("Router did not return a valid structured decision; using GENERAL");
        return new RouteDecision(AgentType.GENERAL, "路由结果无效，已转交通用旅游顾问。");
    }

    private String defaultReason(AgentType agentType) {
        return switch (agentType) {
            case ITINERARY -> "根据用户需求，已路由至行程规划专家。";
            case FOOD -> "根据用户需求，已路由至美食顾问。";
            case BUDGET -> "根据用户需求，已路由至预算顾问。";
            case GENERAL -> "无法明确识别需求，已转交通用旅游顾问。";
        };
    }

    private RuntimeContext context(String sessionId) {
        return RuntimeContext.builder()
                .userId(USER_ID)
                .sessionId(sessionId)
                .build();
    }

    private String specialistSession(String sessionId, AgentType agentType) {
        return "travel-%s-%s".formatted(agentType.name().toLowerCase(Locale.ROOT), sessionId);
    }
}
