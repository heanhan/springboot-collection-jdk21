package com.example.agentscope.travel.context;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.middleware.ReasoningInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证动态运行时提示只进入当前模型输入，不修改原始消息列表。
 */
class TravelRuntimeContextMiddlewareTest {

    private final TravelRuntimeContextMiddleware middleware = new TravelRuntimeContextMiddleware();
    private final Agent agent = mock(Agent.class);

    @Test
    @DisplayName("推理输入临时追加时间、时区和渠道提示，并标记为 synthetic 消息")
    void onReasoning_addsTransientRuntimeReminder() {
        TravelRequestContext requestContext = new TravelRequestContext(
                "request-001", "local-demo-user", "session-001", "http", ZoneId.of("Asia/Shanghai"));
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(requestContext.getUserId())
                .sessionId("travel-food-session-001")
                .put(TravelRequestContext.class, requestContext)
                .build();
        Msg userMessage = Msg.builder()
                .role(MsgRole.USER)
                .textContent("成都三天两夜有什么美食推荐")
                .build();
        ReasoningInput input = new ReasoningInput(
                Collections.singletonList(userMessage), Collections.emptyList(), null);
        List<ReasoningInput> capturedInputs = new ArrayList<ReasoningInput>();

        middleware.onReasoning(agent, runtimeContext, input, nextInput -> {
                    capturedInputs.add(nextInput);
                    return Flux.empty();
                })
                .blockLast();

        assertThat(capturedInputs).hasSize(1);
        List<Msg> messages = capturedInputs.get(0).messages();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isSameAs(userMessage);
        Msg reminder = messages.get(1);
        assertThat(reminder.getTextContent())
                .contains("<system-reminder>")
                .contains("当前时间：")
                .contains("时区：Asia/Shanghai")
                .contains("请求渠道：http")
                .doesNotContain("request-001");
        assertThat(reminder.getMetadata())
                .containsEntry(Msg.METADATA_SYNTHETIC, true)
                .containsEntry(Msg.METADATA_REMINDER_KIND, "travel_runtime_context");
        assertThat(input.messages()).containsExactly(userMessage);
    }

    @Test
    @DisplayName("缺少旅游请求元数据时保持原始推理输入不变")
    void onReasoning_withoutTravelContext_delegatesOriginalInput() {
        ReasoningInput input = new ReasoningInput(
                Collections.emptyList(), Collections.emptyList(), null);
        List<ReasoningInput> capturedInputs = new ArrayList<ReasoningInput>();

        middleware.onReasoning(agent, RuntimeContext.empty(), input, nextInput -> {
                    capturedInputs.add(nextInput);
                    return Flux.empty();
                })
                .blockLast();

        assertThat(capturedInputs).containsExactly(input);
    }
}
