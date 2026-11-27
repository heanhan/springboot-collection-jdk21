package com.example.agentscope.travel.context;

import com.example.agentscope.travel.api.AgentType;
import com.example.agentscope.travel.config.TravelContextProperties;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证一次请求的元数据在 Router、专家和 Guide 之间一致传递。
 */
class TravelRuntimeContextFactoryTest {

    private final TravelRuntimeContextFactory factory =
            new TravelRuntimeContextFactory(new TravelContextProperties());

    @Test
    @DisplayName("同一请求共享元数据，Agent 会话 ID 使用独立命名空间")
    void contextsForOneRequest_shareMetadataAndKeepAgentSessionsSeparate() {
        TravelRequestContext requestContext = factory.createRequest("chengdu-001");
        RuntimeContext routerContext = factory.forRouter(requestContext);
        RuntimeContext specialistContext = factory.forSpecialist(requestContext, AgentType.FOOD);
        RuntimeContext guideContext = factory.forGuide(requestContext);

        assertThat(requestContext.getRequestId()).isNotBlank();
        assertThat(requestContext.getUserId()).isEqualTo("local-demo-user");
        assertThat(requestContext.getSessionId()).isEqualTo("chengdu-001");
        assertThat(requestContext.getChannel()).isEqualTo("http");
        assertThat(requestContext.getZoneId().getId()).isEqualTo("Asia/Shanghai");
        assertThat(routerContext.get(TravelRequestContext.class)).isSameAs(requestContext);
        assertThat(specialistContext.get(TravelRequestContext.class)).isSameAs(requestContext);
        assertThat(guideContext.get(TravelRequestContext.class)).isSameAs(requestContext);
        assertThat(routerContext.getSessionId()).isEqualTo("travel-router-chengdu-001");
        assertThat(specialistContext.getSessionId()).isEqualTo("travel-food-chengdu-001");
        assertThat(guideContext.getSessionId()).isEqualTo("travel-guide-chengdu-001");
        assertThat(routerContext.getUserId()).isEqualTo(requestContext.getUserId());
        assertThat(routerContext.<String>get(TravelRuntimeContextFactory.REQUEST_ID_ATTRIBUTE))
                .isEqualTo(requestContext.getRequestId());
    }

    @Test
    @DisplayName("不同请求生成独立 requestId 和上下文属性")
    void differentRequests_doNotShareRuntimeMetadata() {
        TravelRequestContext firstRequest = factory.createRequest("session-001");
        TravelRequestContext secondRequest = factory.createRequest("session-002");
        RuntimeContext firstContext = factory.forRouter(firstRequest);
        RuntimeContext secondContext = factory.forRouter(secondRequest);

        assertThat(firstRequest.getRequestId()).isNotEqualTo(secondRequest.getRequestId());
        assertThat(firstContext.get(TravelRequestContext.class)).isSameAs(firstRequest);
        assertThat(secondContext.get(TravelRequestContext.class)).isSameAs(secondRequest);
        assertThat(firstContext.<String>get(TravelRuntimeContextFactory.REQUEST_ID_ATTRIBUTE))
                .isNotEqualTo(secondContext.<String>get(TravelRuntimeContextFactory.REQUEST_ID_ATTRIBUTE));
    }
}
