package com.example.agentscope.travel.service;

import com.example.agentscope.travel.agent.TravelAgentTeam;
import com.example.agentscope.travel.api.AgentType;
import com.example.agentscope.travel.api.KnowledgeSearchRequest;
import com.example.agentscope.travel.api.KnowledgeSearchResponse;
import com.example.agentscope.travel.api.KnowledgeSearchResult;
import com.example.agentscope.travel.api.RouteDecision;
import com.example.agentscope.travel.api.TravelChatRequest;
import com.example.agentscope.travel.api.TravelChatResponse;
import com.example.agentscope.travel.config.TravelAgentProperties;
import com.example.agentscope.travel.rag.TravelKnowledgeService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 RAG 材料、专家回答和最终来源引用之间的传递关系。
 */
@ExtendWith(MockitoExtension.class)
class TravelGuideServiceTest {

    @Mock
    private HarnessAgent router;

    @Mock
    private HarnessAgent foodAgent;

    @Mock
    private HarnessAgent guideAgent;

    @Mock
    private TravelKnowledgeService knowledgeService;

    private TravelGuideService travelGuideService;

    @BeforeEach
    void setUp() {
        TravelAgentTeam team = new TravelAgentTeam(
                router,
                Collections.singletonMap(AgentType.FOOD, foodAgent),
                guideAgent);
        TravelAgentProperties properties = new TravelAgentProperties();
        properties.setTimeoutSeconds(10);
        travelGuideService = spy(new TravelGuideService(
                team, knowledgeService, properties, MemoryConfig.defaults()));
        doNothing().when(travelGuideService).scheduleMemoryRepair(any(RuntimeContext.class));

        Msg routeMessage = mock(Msg.class);
        RouteDecision decision = new RouteDecision(AgentType.FOOD, "用户想了解成都美食。");
        when(routeMessage.hasStructuredData()).thenReturn(true);
        when(routeMessage.getStructuredData(RouteDecision.class)).thenReturn(decision);
        when(router.call(anyList(), eq(RouteDecision.class), any(RuntimeContext.class)))
                .thenReturn(Mono.just(routeMessage));
    }

    @Test
    @DisplayName("检索知识按相同编号进入专家、指南和最终来源列表")
    void chatWithKnowledge_injectsNumberedSourcesAndReturnsCitations() {
        List<KnowledgeSearchResult> sources = Arrays.asList(
                new KnowledgeSearchResult(
                        "chengdu-food-1", "成都美食指南 - 代表性美食",
                        "chengdu-food.md", 12, "可以了解火锅、串串和小吃。"),
                new KnowledgeSearchResult(
                        "travel-safety-1", "旅行安全 - 饮食卫生",
                        "travel-safety.md", 5, "注意饮食卫生，出现不适及时就医。"));
        KnowledgeSearchResponse knowledgeResponse = new KnowledgeSearchResponse("成都美食", 3, sources);
        when(knowledgeService.search(any(KnowledgeSearchRequest.class))).thenReturn(knowledgeResponse);
        when(foodAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message("成都美食可从火锅和小吃入手。[1]")));
        when(guideAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message("成都美食可从火锅和小吃入手。[1] 同时注意饮食卫生。[2]")));

        TravelChatResponse response = travelGuideService.chat(request());

        ArgumentCaptor<KnowledgeSearchRequest> retrievalCaptor =
                ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
        verify(knowledgeService).search(retrievalCaptor.capture());
        assertThat(retrievalCaptor.getValue().getQuery())
                .isEqualTo("成都三天两夜有什么美食推荐")
                .doesNotContain("用户想了解成都美食");
        assertThat(retrievalCaptor.getValue().getTopK()).isZero();

        ArgumentCaptor<String> specialistInputCaptor = ArgumentCaptor.forClass(String.class);
        verify(foodAgent).call(specialistInputCaptor.capture(), any(RuntimeContext.class));
        assertThat(specialistInputCaptor.getValue())
                .contains("[1] 成都美食指南 - 代表性美食")
                .contains("[2] 旅行安全 - 饮食卫生")
                .contains("来源文件：chengdu-food.md")
                .contains("采用某条知识时在对应句子后标注 [1]、[2] 等编号")
                .contains("用户长期记忆（个人上下文，禁止添加知识引用；当前用户消息优先）");

        ArgumentCaptor<String> guideInputCaptor = ArgumentCaptor.forClass(String.class);
        verify(guideAgent).call(guideInputCaptor.capture(), any(RuntimeContext.class));
        assertThat(guideInputCaptor.getValue())
                .contains("引用要求：保留专家材料中的 [1]、[2] 等知识引用编号")
                .contains("描述用户长期记忆的句子不得添加引用编号")
                .contains("片段编号：travel-safety-1");

        assertThat(response.getKnowledgeSources()).containsExactlyElementsOf(sources);
        assertThat(response.getAnswer())
                .contains("成都美食可从火锅和小吃入手。[1]")
                .contains("注意饮食卫生。[2]")
                .contains("### 知识来源")
                .contains("- [1] `chengdu-food.md`")
                .contains("- [2] `travel-safety.md`");
        verify(travelGuideService).scheduleMemoryRepair(any(RuntimeContext.class));
    }

    @Test
    @DisplayName("没有知识命中时明确告知 Agent 且不生成知识来源")
    void chatWithoutKnowledge_marksNoKnowledgeAndReturnsNoSources() {
        KnowledgeSearchResponse knowledgeResponse = new KnowledgeSearchResponse(
                "成都美食", 3, new ArrayList<KnowledgeSearchResult>());
        when(knowledgeService.search(any(KnowledgeSearchRequest.class))).thenReturn(knowledgeResponse);
        when(foodAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message("可以介绍常见的成都美食方向。")));
        when(guideAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message("可以介绍常见的成都美食方向。")));

        TravelChatResponse response = travelGuideService.chat(request());

        ArgumentCaptor<String> specialistInputCaptor = ArgumentCaptor.forClass(String.class);
        verify(foodAgent).call(specialistInputCaptor.capture(), any(RuntimeContext.class));
        assertThat(specialistInputCaptor.getValue())
                .contains("未检索到与当前问题匹配的项目内置知识");
        assertThat(response.getKnowledgeSources()).isEmpty();
        assertThat(response.getAnswer()).doesNotContain("### 知识来源");
    }

    @Test
    @DisplayName("指南 Agent 返回空内容时仍使用专家回答并保留来源")
    void chatWhenGuideReturnsBlank_fallsBackToSpecialistAndKeepsSources() {
        KnowledgeSearchResult source = new KnowledgeSearchResult(
                "chengdu-food-1", "成都美食指南 - 代表性美食",
                "chengdu-food.md", 12, "可以了解火锅、串串和小吃。");
        KnowledgeSearchResponse knowledgeResponse = new KnowledgeSearchResponse(
                "成都美食", 3, Collections.singletonList(source));
        when(knowledgeService.search(any(KnowledgeSearchRequest.class))).thenReturn(knowledgeResponse);
        when(foodAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message("成都美食可从火锅入手。[1]")));
        when(guideAgent.call(anyString(), any(RuntimeContext.class)))
                .thenReturn(Mono.just(message(" ")));

        TravelChatResponse response = travelGuideService.chat(request());

        assertThat(response.getKnowledgeSources()).containsExactly(source);
        assertThat(response.getAnswer())
                .startsWith("成都美食可从火锅入手。[1]")
                .contains("### 知识来源")
                .contains("- [1] `chengdu-food.md`");
    }

    private TravelChatRequest request() {
        return new TravelChatRequest("rag-test-session", "成都三天两夜有什么美食推荐");
    }

    private Msg message(String text) {
        return Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(text)
                .build();
    }
}
