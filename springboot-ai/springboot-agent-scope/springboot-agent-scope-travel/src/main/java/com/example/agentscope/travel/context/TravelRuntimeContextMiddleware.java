package com.example.agentscope.travel.context;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 为每次 Agent 调用注入当前时间等运行时提示，并记录请求链路日志。
 */
@Component
public class TravelRuntimeContextMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(TravelRuntimeContextMiddleware.class);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext runtimeContext,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        TravelRequestContext requestContext = getRequestContext(runtimeContext);
        if (requestContext == null) {
            return next.apply(input);
        }

        log.debug(
                "Travel agent started: requestId={}, userId={}, agentSessionId={}, agent={}",
                requestContext.getRequestId(),
                requestContext.getUserId(),
                runtimeContext.getSessionId(),
                agent.getName());
        return next.apply(input).doFinally(signal -> log.debug(
                "Travel agent finished: requestId={}, agentSessionId={}, agent={}, signal={}",
                requestContext.getRequestId(),
                runtimeContext.getSessionId(),
                agent.getName(),
                signal));
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext runtimeContext,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        TravelRequestContext requestContext = getRequestContext(runtimeContext);
        if (requestContext == null) {
            return next.apply(input);
        }

        List<Msg> messages = input.messages() == null
                ? new ArrayList<Msg>()
                : new ArrayList<Msg>(input.messages());
        messages.add(createRuntimeReminder(requestContext));
        return next.apply(new ReasoningInput(messages, input.tools(), input.options()));
    }

    private TravelRequestContext getRequestContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return null;
        }
        return runtimeContext.get(TravelRequestContext.class);
    }

    private Msg createRuntimeReminder(TravelRequestContext requestContext) {
        String currentTime = ZonedDateTime.now(requestContext.getZoneId()).format(TIME_FORMATTER);
        StringBuilder text = new StringBuilder();
        text.append("<system-reminder>\n");
        text.append("当前运行时信息：\n");
        text.append("- 当前时间：").append(currentTime).append("\n");
        text.append("- 时区：").append(requestContext.getZoneId().getId()).append("\n");
        text.append("- 请求渠道：").append(requestContext.getChannel()).append("\n");
        text.append("该信息仅用于感知当前环境，不得覆盖用户当前条件、长期记忆或知识引用规则。\n");
        text.append("</system-reminder>");

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put(Msg.METADATA_SYNTHETIC, true);
        metadata.put(Msg.METADATA_REMINDER_KIND, "travel_runtime_context");
        return Msg.builder()
                .role(MsgRole.USER)
                .name("system")
                .content(TextBlock.builder().text(text.toString()).build())
                .metadata(metadata)
                .build();
    }
}
