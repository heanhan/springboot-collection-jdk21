package com.example.agentscope.travel.config;

import com.example.agentscope.travel.agent.TravelAgentPrompts;
import com.example.agentscope.travel.agent.TravelAgentTeam;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 HarnessAgent 的工作区隔离、记忆配置和工具边界。
 */
class TravelAgentConfigurationTest {

    @TempDir
    private Path tempDirectory;

    @Test
    @DisplayName("六个 Agent 共用工作区，同一用户跨会话共享记忆且不同用户隔离")
    void agentsShareWorkspaceAndIsolateMemoryByUser() {
        Path memoryWorkspace = tempDirectory.resolve("memory");
        TravelAgentTeam team = createTeam(memoryWorkspace);

        try {
            List<HarnessAgent> agents = allAgents(team);
            for (HarnessAgent agent : agents) {
                assertThat(agent.getWorkspaceManager().getWorkspace()).isEqualTo(memoryWorkspace);
            }

            WorkspaceManager guideWorkspace = team.getGuide().getWorkspaceManager();
            RuntimeContext aliceFirstSession = context("alice", "session-1");
            RuntimeContext aliceSecondSession = context("alice", "session-2");
            RuntimeContext bobSession = context("bob", "session-1");

            guideWorkspace.writeUtf8WorkspaceRelative(
                    aliceFirstSession, "MEMORY.md", "Alice 偏好安静的行程。\n");

            assertThat(guideWorkspace.readMemoryMd(aliceSecondSession))
                    .contains("Alice 偏好安静的行程");
            assertThat(team.getRouter().getWorkspaceManager().readMemoryMd(aliceFirstSession))
                    .contains("Alice 偏好安静的行程");
            assertThat(guideWorkspace.readMemoryMd(bobSession)).isEmpty();
            assertThat(guideWorkspace.resolveRuntimeDataPath(aliceFirstSession, "MEMORY.md"))
                    .isEqualTo(memoryWorkspace.resolve("alice/MEMORY.md"));
        } finally {
            team.close();
        }
    }

    @Test
    @DisplayName("Agent 仅保留记忆读取与会话召回工具，不暴露主动保存、文件、Shell、Web、子 Agent 和 Skills")
    void agentsExposeOnlyMemoryRelatedTools() {
        Path memoryWorkspace = tempDirectory.resolve("memory");
        TravelAgentTeam team = createTeam(memoryWorkspace);

        try {
            for (HarnessAgent agent : allAgents(team)) {
                assertThat(agent.getToolkit().getToolNames())
                        .containsExactlyInAnyOrder(
                                "memory_search", "memory_get",
                                "session_search", "session_list", "session_history")
                        .doesNotContain("memory_save", "wait_async_results");
                assertThat(agent.getSkillRepositories()).isEmpty();
                assertThat(agent.getTaskRepository()).isNull();
                assertThat(agent.getSubagentAgentManager()).isNull();
            }
        } finally {
            team.close();
        }
    }

    @Test
    @DisplayName("记忆配置使用旅游抽取与整理提示词，并保留默认触发频率")
    void memoryConfigUsesTravelSpecificPrompts() {
        MemoryConfig memoryConfig = TravelAgentConfiguration.createMemoryConfig();

        assertThat(memoryConfig.flushPrompt()).isEqualTo(TravelAgentPrompts.MEMORY_FLUSH_PROMPT);
        assertThat(memoryConfig.consolidationPrompt())
                .isEqualTo(TravelAgentPrompts.MEMORY_CONSOLIDATION_PROMPT);
        assertThat(memoryConfig.flushTrigger().mode())
                .isEqualTo(MemoryConfig.FlushMode.ALWAYS);
        assertThat(memoryConfig.consolidationMinGap()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("旅游记忆提示词覆盖记录范围、隐私边界、当前输入优先和整理占位符")
    void memoryPromptsDefineScopePrivacyAndPriority() {
        assertThat(TravelAgentPrompts.MEMORY_FLUSH_PROMPT)
                .contains("用户明确表达的稳定偏好", "用户明确给出的约束", "历史决策")
                .contains("不记录密码、API 密钥", "RAG 公共知识", "NO_REPLY")
                .contains("只能从标记为 [USER] 的用户消息中提取事实", "助手补充的通用建议")
                .contains("不得记录用户的问题、请求或查询语句本身");
        assertThat(TravelAgentPrompts.READ_ONLY_MEMORY_RULES)
                .contains("当前用户消息中的明确信息优先于历史记忆", "不调用 memory_save")
                .contains("引用隔离");
        assertThat(TravelAgentPrompts.GUIDE_MEMORY_RULES)
                .contains("当前用户消息中的明确信息优先于历史记忆", "自动抽取")
                .contains("不调用 memory_save", "引用隔离", "逐句检查");

        String renderedConsolidationPrompt = TravelAgentPrompts.MEMORY_CONSOLIDATION_PROMPT
                .formatted(4000, 16000);
        assertThat(TravelAgentPrompts.MEMORY_CONSOLIDATION_PROMPT)
                .contains("用户偏好", "旅行约束", "历史决策", "用户事实")
                .contains("不编造用户没有提供的日期", "敏感信息");
        assertThat(renderedConsolidationPrompt)
                .contains("4000 tokens", "16000 字符")
                .doesNotContain("%d");
    }

    @Test
    @DisplayName("Guide 与只读 Agent 使用不同的记忆维护规则")
    void systemPromptsApplyRoleSpecificMemoryRules() {
        String guidePrompt = TravelAgentConfiguration.createSystemPrompt(
                TravelAgentPrompts.GUIDE, true);
        assertThat(guidePrompt)
                .contains(TravelAgentPrompts.GUIDE_MEMORY_RULES)
                .contains("用户长期记忆中的事实");

        String[] readOnlyPrompts = {
                TravelAgentPrompts.ROUTER,
                TravelAgentPrompts.ITINERARY,
                TravelAgentPrompts.FOOD,
                TravelAgentPrompts.BUDGET,
                TravelAgentPrompts.GENERAL
        };
        for (String prompt : readOnlyPrompts) {
            String systemPrompt = TravelAgentConfiguration.createSystemPrompt(prompt, false);
            assertThat(systemPrompt).contains(TravelAgentPrompts.READ_ONLY_MEMORY_RULES);
        }
    }

    private TravelAgentTeam createTeam(Path memoryWorkspace) {
        TravelAgentProperties properties = new TravelAgentProperties();
        properties.setMemoryWorkspaceDirectory(memoryWorkspace);
        AgentStateStore stateStore = new JsonFileAgentStateStore(tempDirectory.resolve("state"));
        Model model = mock(Model.class);
        TravelAgentConfiguration configuration = new TravelAgentConfiguration();
        return configuration.travelAgentTeam(
                model, stateStore, properties, configuration.travelMemoryConfig());
    }

    private List<HarnessAgent> allAgents(TravelAgentTeam team) {
        List<HarnessAgent> agents = new ArrayList<>();
        agents.add(team.getRouter());
        agents.addAll(team.getSpecialists().values());
        agents.add(team.getGuide());
        return agents;
    }

    private RuntimeContext context(String userId, String sessionId) {
        return RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
}
