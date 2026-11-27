package com.example.agentscope.travel.rag;

import com.example.agentscope.travel.config.TravelEmbeddingProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;

/**
 * 通过 OpenAI 兼容 /embeddings 接口调用本地 Qwen3 Embedding 模型。
 */
@Service
@Profile("!demo-embedding")
public class OpenAiCompatibleEmbeddingService implements TravelEmbeddingService {

    private final TravelEmbeddingProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleEmbeddingService(
            TravelEmbeddingProperties properties,
            @Qualifier("travelEmbeddingRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public int getDimension() {
        return properties.getDimension();
    }

    @Override
    public String getModelName() {
        return properties.getModelName() + "@" + properties.getDimension();
    }

    @Override
    public float[] embedDocument(String text) {
        return requestEmbedding(validateText(text));
    }

    @Override
    public float[] embedQuery(String text) {
        String query = validateText(text);
        String input = properties.getQueryInstruction().trim() + "\nQuery: " + query;
        return requestEmbedding(input);
    }

    private float[] requestEmbedding(String input) {
        OpenAiEmbeddingRequest requestBody = new OpenAiEmbeddingRequest(
                properties.getModelName(), input, properties.getDimension());
        OpenAiEmbeddingResponse response;
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(createEmbeddingsUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);
            String apiKey = properties.getApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
            }
            response = request.body(requestBody)
                    .retrieve()
                    .body(OpenAiEmbeddingResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "调用 Embedding 服务失败，请检查 travel.rag.embedding 配置和本地服务状态。",
                    exception);
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new IllegalStateException("Embedding 服务未返回向量数据。");
        }
        OpenAiEmbeddingResponse.EmbeddingData embeddingData = findFirstEmbedding(response.getData());
        if (embeddingData == null) {
            throw new IllegalStateException("Embedding 服务未返回 index 为 0 的向量数据。");
        }
        return convertToVector(embeddingData.getEmbedding());
    }

    private URI createEmbeddingsUri() {
        String baseUrl = properties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return URI.create(baseUrl + "/embeddings");
    }

    private OpenAiEmbeddingResponse.EmbeddingData findFirstEmbedding(
            List<OpenAiEmbeddingResponse.EmbeddingData> dataList) {
        for (OpenAiEmbeddingResponse.EmbeddingData data : dataList) {
            if (data != null && data.getIndex() == 0) {
                return data;
            }
        }
        return null;
    }

    private float[] convertToVector(List<Float> values) {
        if (values == null || values.size() != properties.getDimension()) {
            int actualDimension = values == null ? 0 : values.size();
            throw new IllegalStateException(
                    "Embedding 维度必须为 " + properties.getDimension()
                            + "，实际为 " + actualDimension + "。");
        }

        float[] vector = new float[properties.getDimension()];
        for (int index = 0; index < values.size(); index++) {
            Float value = values.get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new IllegalStateException("Embedding 向量包含空值或非有限数值。");
            }
            vector[index] = value;
        }
        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double squaredMagnitude = 0.0D;
        for (float value : vector) {
            squaredMagnitude += value * value;
        }
        double magnitude = Math.sqrt(squaredMagnitude);
        if (magnitude == 0.0D || !Double.isFinite(magnitude)) {
            throw new IllegalStateException("Embedding 向量不能是零向量或超出数值范围。");
        }
        for (int index = 0; index < vector.length; index++) {
            vector[index] = (float) (vector[index] / magnitude);
        }
    }

    private String validateText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Embedding 输入文本不能为空");
        }
        return text.trim();
    }
}
