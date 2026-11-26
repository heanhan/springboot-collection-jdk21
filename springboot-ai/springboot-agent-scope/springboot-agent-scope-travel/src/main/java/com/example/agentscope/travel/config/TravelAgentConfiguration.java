package com.example.agentscope.travel.config;

import com.example.agentscope.travel.agent.TravelAgentPrompts;
import com.example.agentscope.travel.agent.TravelAgentTeam;
import com.example.agentscope.travel.api.AgentType;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({TravelAgentProperties.class, TravelRagProperties.class})
public class TravelAgentConfiguration {

    @Bean
    public Model travelModel(TravelAgentProperties properties) {
        return OpenAIChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModelName())
                .stream(properties.isStream())
                .formatter(new OpenAIChatFormatter())
                .generateOptions(GenerateOptions.builder()
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .parallelToolCalls(false)
                        .build())
                .build();
    }

    @Bean
    public AgentStateStore travelStateStore(TravelAgentProperties properties) {
        return new JsonFileAgentStateStore(properties.getStateDirectory());
    }

    @Bean
    public TravelAgentTeam travelAgentTeam(Model model, AgentStateStore stateStore) {
        ReActAgent router = createAgent(
                "travel-router", TravelAgentPrompts.ROUTER, model, stateStore, 3);
        ReActAgent guide = createAgent(
                "travel-guide", TravelAgentPrompts.GUIDE, model, stateStore, 4);

        Map<AgentType, ReActAgent> specialists = new EnumMap<>(AgentType.class);
        specialists.put(AgentType.ITINERARY, createAgent(
                "travel-itinerary", TravelAgentPrompts.ITINERARY, model, stateStore, 4));
        specialists.put(AgentType.FOOD, createAgent(
                "travel-food", TravelAgentPrompts.FOOD, model, stateStore, 4));
        specialists.put(AgentType.BUDGET, createAgent(
                "travel-budget", TravelAgentPrompts.BUDGET, model, stateStore, 4));
        specialists.put(AgentType.GENERAL, createAgent(
                "travel-general", TravelAgentPrompts.GENERAL, model, stateStore, 4));

        return new TravelAgentTeam(router, Map.copyOf(specialists), guide);
    }

    private ReActAgent createAgent(
            String name, String prompt, Model model, AgentStateStore stateStore, int maxIters) {
        return ReActAgent.builder()
                .name(name)
                .sysPrompt(prompt)
                .model(model)
                .toolkit(new Toolkit())
                .stateStore(stateStore)
                .maxIters(maxIters)
                .build();
    }
}
