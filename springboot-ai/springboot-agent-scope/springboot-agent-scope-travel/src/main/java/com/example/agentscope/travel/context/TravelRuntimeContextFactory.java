package com.example.agentscope.travel.context;

import com.example.agentscope.travel.api.AgentType;
import com.example.agentscope.travel.config.TravelContextProperties;
import io.agentscope.core.agent.RuntimeContext;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

/**
 * 统一创建一次请求在 Router、专家和 Guide 中的运行时上下文。
 */
@Component
public class TravelRuntimeContextFactory {

    private static final String DEMO_USER_ID = "local-demo-user";
    private static final String HTTP_CHANNEL = "http";
    public static final String REQUEST_ID_ATTRIBUTE = "request_id";

    private final ZoneId zoneId;

    public TravelRuntimeContextFactory(TravelContextProperties properties) {
        this.zoneId = ZoneId.of(properties.getTimeZone());
    }

    public TravelRequestContext createRequest(String sessionId) {
        return new TravelRequestContext(
                UUID.randomUUID().toString(),
                DEMO_USER_ID,
                sessionId,
                HTTP_CHANNEL,
                zoneId);
    }

    public RuntimeContext forRouter(TravelRequestContext requestContext) {
        return createAgentContext(
                requestContext, "travel-router-" + requestContext.getSessionId());
    }

    public RuntimeContext forSpecialist(
            TravelRequestContext requestContext, AgentType agentType) {
        String agentName = agentType.name().toLowerCase(Locale.ROOT);
        return createAgentContext(
                requestContext, "travel-" + agentName + "-" + requestContext.getSessionId());
    }

    public RuntimeContext forGuide(TravelRequestContext requestContext) {
        return createAgentContext(
                requestContext, "travel-guide-" + requestContext.getSessionId());
    }

    private RuntimeContext createAgentContext(
            TravelRequestContext requestContext, String agentSessionId) {
        return RuntimeContext.builder()
                .userId(requestContext.getUserId())
                .sessionId(agentSessionId)
                .put(TravelRequestContext.class, requestContext)
                .put(REQUEST_ID_ATTRIBUTE, requestContext.getRequestId())
                .build();
    }
}
