package com.example.agentscope.travel.rag;

import com.example.agentscope.travel.config.TravelEmbeddingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 真实 Embedding HTTP 适配器测试，不访问本地模型服务。
 */
class OpenAiCompatibleEmbeddingServiceTest {

    private RestClient restClient;
    private MockRestServiceServer server;
    private TravelEmbeddingProperties properties;
    private OpenAiCompatibleEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        properties = new TravelEmbeddingProperties();
        properties.setBaseUrl("http://embedding.test/v1/");
        properties.setModelName("Qwen3-Embedding-0.6B");
        properties.setDimension(3);
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        restClient = restClientBuilder.build();
        embeddingService = new OpenAiCompatibleEmbeddingService(properties, restClient);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    @DisplayName("模型标记包含模型名称和维度，避免不同向量混用")
    void metadata_containsModelNameAndDimension() {
        assertThat(embeddingService.getDimension()).isEqualTo(3);
        assertThat(embeddingService.getModelName()).isEqualTo("Qwen3-Embedding-0.6B@3");
    }

    @Test
    @DisplayName("文档请求发送模型、维度和原文，并归一化返回向量")
    void embedDocument_sendsDocumentWithoutQueryInstruction() {
        properties.setApiKey("test-key");
        server.expect(requestTo("http://embedding.test/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("Qwen3-Embedding-0.6B"))
                .andExpect(jsonPath("$.input").value("成都美食"))
                .andExpect(jsonPath("$.dimensions").value(3))
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[3,4,0]}]}",
                        MediaType.APPLICATION_JSON));

        float[] vector = embeddingService.embedDocument("成都美食");

        assertThat(vector).containsExactly(0.6F, 0.8F, 0.0F);
    }

    @Test
    @DisplayName("查询请求在原文前添加 Qwen3 检索指令")
    void embedQuery_addsConfiguredInstruction() {
        properties.setQueryInstruction("Instruct: retrieve travel knowledge");
        expectEmbeddingRequest("Instruct: retrieve travel knowledge\nQuery: 成都美食")
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[3,4,0]}]}",
                        MediaType.APPLICATION_JSON));

        float[] vector = embeddingService.embedQuery("成都美食");

        assertThat(vector).containsExactly(0.6F, 0.8F, 0.0F);
    }

    @Test
    @DisplayName("未配置密钥时不发送 Authorization 请求头")
    void embedDocument_withoutApiKeyOmitsAuthorizationHeader() {
        server.expect(requestTo("http://embedding.test/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(
                        request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull())
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[1,0,0]}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(embeddingService.embedDocument("文档")).hasSize(3);
    }

    @Test
    @DisplayName("空文本在发送 HTTP 请求前被拒绝")
    void embedWithBlankText_throwsBeforeRequest() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> embeddingService.embedDocument("  "))
                .withMessageContaining("不能为空");
    }

    @Test
    @DisplayName("返回向量维度与配置不一致时明确失败")
    void embedWithWrongDimension_throwsException() {
        expectEmbeddingRequest("成都美食")
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[1,2]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> embeddingService.embedDocument("成都美食"))
                .withMessageContaining("维度必须为 3");
    }

    @Test
    @DisplayName("返回向量包含空值或非有限数值时明确失败")
    void embedWithInvalidNumber_throwsException() {
        expectEmbeddingRequest("成都美食")
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[1,null,3]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> embeddingService.embedDocument("成都美食"))
                .withMessageContaining("空值或非有限数值");
    }

    @Test
    @DisplayName("零向量不能写入 pgvector")
    void embedWithZeroVector_throwsException() {
        expectEmbeddingRequest("成都美食")
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[0,0,0]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> embeddingService.embedDocument("成都美食"))
                .withMessageContaining("不能是零向量");
    }

    @Test
    @DisplayName("响应缺少 index 为 0 的数据时明确失败")
    void embedWithMissingIndex_throwsException() {
        expectEmbeddingRequest("成都美食")
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":1,\"embedding\":[1,0,0]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatIllegalStateException()
                .isThrownBy(() -> embeddingService.embedDocument("成都美食"))
                .withMessageContaining("index 为 0");
    }

    @Test
    @DisplayName("Embedding 服务返回 HTTP 错误时转换为中文异常")
    void embedWhenServiceReturnsError_throwsChineseException() {
        expectEmbeddingRequest("成都美食")
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatIllegalStateException()
                .isThrownBy(() -> embeddingService.embedDocument("成都美食"))
                .withMessageContaining("调用 Embedding 服务失败");
    }

    private ResponseActions expectEmbeddingRequest(String expectedInput) {
        return server.expect(requestTo("http://embedding.test/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("Qwen3-Embedding-0.6B"))
                .andExpect(jsonPath("$.input").value(expectedInput))
                .andExpect(jsonPath("$.dimensions").value(3));
    }
}
