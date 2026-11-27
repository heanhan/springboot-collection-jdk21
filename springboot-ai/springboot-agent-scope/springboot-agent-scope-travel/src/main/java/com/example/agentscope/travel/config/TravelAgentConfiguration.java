package com.example.agentscope.travel.config;

import com.example.agentscope.travel.agent.TravelAgentPrompts;
import com.example.agentscope.travel.agent.TravelAgentTeam;
import com.example.agentscope.travel.api.AgentType;
import com.example.agentscope.travel.context.TravelRuntimeContextMiddleware;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.workspace.LocalFsMode;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        TravelAgentProperties.class,
        TravelRagProperties.class,
        TravelContextProperties.class
})
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
    public MemoryConfig travelMemoryConfig() {
        return createMemoryConfig();
    }

    @Bean
    public TravelAgentTeam travelAgentTeam(
            Model model,
            AgentStateStore stateStore,
            TravelAgentProperties properties,
            MemoryConfig memoryConfig,
            TravelRuntimeContextMiddleware contextMiddleware) {
        Path memoryWorkspace = properties.getMemoryWorkspaceDirectory();
        HarnessAgent router = createAgent(
                "travel-router", TravelAgentPrompts.ROUTER,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 3, false);
        HarnessAgent guide = createAgent(
                "travel-guide", TravelAgentPrompts.GUIDE,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 4, true);

        Map<AgentType, HarnessAgent> specialists = new EnumMap<>(AgentType.class);
        specialists.put(AgentType.ITINERARY, createAgent(
                "travel-itinerary", TravelAgentPrompts.ITINERARY,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 4, false));
        specialists.put(AgentType.FOOD, createAgent(
                "travel-food", TravelAgentPrompts.FOOD,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 4, false));
        specialists.put(AgentType.BUDGET, createAgent(
                "travel-budget", TravelAgentPrompts.BUDGET,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 4, false));
        specialists.put(AgentType.GENERAL, createAgent(
                "travel-general", TravelAgentPrompts.GENERAL,
                model, stateStore, memoryWorkspace, memoryConfig, contextMiddleware, 4, false));

        return new TravelAgentTeam(router, Map.copyOf(specialists), guide);
    }

    private HarnessAgent createAgent(
            String name,
            String prompt,
            Model model,
            AgentStateStore stateStore,
            Path memoryWorkspace,
            MemoryConfig memoryConfig,
            TravelRuntimeContextMiddleware contextMiddleware,
            int maxIters,
            boolean automaticMemory) {
        // 项目目录也指向记忆工作区，避免 LocalFilesystemSpec 默认回退读取当前应用源码。
        LocalFilesystemSpec filesystem = new LocalFilesystemSpec()
                .project(memoryWorkspace)
                .mode(LocalFsMode.SANDBOXED)
                .isolationScope(IsolationScope.USER);

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name(name)
                .sysPrompt(createSystemPrompt(prompt, automaticMemory))
                .model(model)
                .middleware(contextMiddleware)
                .toolkit(new Toolkit())
                .stateStore(stateStore)
                .maxIters(maxIters)
                .workspace(memoryWorkspace)
                .filesystem(filesystem)
                .memory(memoryConfig)
                // 旅游 Agent 只回答问题和使用记忆，不提供编码、联网或继续派生子 Agent 的能力。
                .disableCompaction()
                .disableToolResultEviction()
                .disableFilesystemTools()
                .disableShellTool()
                .disableWebTools()
                .disableSubagents()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .skillsEnabled(false)
                .disableToolsConfig()
                .disableAtPathExpansion()
                .enableAgentTracingLog(false);

        if (!automaticMemory) {
            // 路由和专家不自动抽取记忆，自动整理统一交给 Guide，避免重复调用模型和内容冲突。
            builder.disableMemoryHooks();
        }
        HarnessAgent agent = builder.build();
        // 工作区会自动创建消息总线，但旅游 Agent 没有子任务，因此移除无关的异步等待工具。
        agent.getToolkit().removeTool("wait_async_results");
        // 长期记忆统一由 Guide 的自动 hooks 写入，移除主动保存工具，避免模型重复写入。
        agent.getToolkit().removeTool("memory_save");
        return agent;
    }

    /**
     * Guide 负责维护记忆，路由和专家只读取，避免多个 Agent 重复写入同一份用户信息。
     */
    static String createSystemPrompt(String prompt, boolean automaticMemory) {
        String memoryRules = automaticMemory
                ? TravelAgentPrompts.GUIDE_MEMORY_RULES
                : TravelAgentPrompts.READ_ONLY_MEMORY_RULES;
        return prompt + "\n\n" + memoryRules;
    }

    /**
     * 使用旅游场景的抽取和整理提示词，其余频率与保留策略沿用 Harness 默认值。
     */
    static MemoryConfig createMemoryConfig() {
        return MemoryConfig.builder()
                .flushPrompt(TravelAgentPrompts.MEMORY_FLUSH_PROMPT)
                .consolidationPrompt(TravelAgentPrompts.MEMORY_CONSOLIDATION_PROMPT)
                .flushTrigger(MemoryConfig.FlushTrigger.always())
                .build();
    }
}
