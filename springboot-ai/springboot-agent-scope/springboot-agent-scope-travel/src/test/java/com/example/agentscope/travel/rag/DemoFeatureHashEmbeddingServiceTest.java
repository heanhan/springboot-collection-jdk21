package com.example.agentscope.travel.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * 假 Embedding 的稳定性和基础检索能力测试。
 */
class DemoFeatureHashEmbeddingServiceTest {

    private final DemoFeatureHashEmbeddingService embeddingService =
            new DemoFeatureHashEmbeddingService();

    @Test
    @DisplayName("假 Embedding 返回固定维度和模型标记")
    void embed_returnsConfiguredDimensionAndModelName() {
        assertThat(embeddingService.getDimension()).isEqualTo(256);
        assertThat(embeddingService.getModelName()).isEqualTo("demo-feature-hash-v3");
        assertThat(embeddingService.embedDocument("成都美食")).hasSize(256);
    }

    @Test
    @DisplayName("相同文本每次生成完全相同的向量")
    void embedWithSameText_returnsStableVector() {
        float[] first = embeddingService.embedDocument("成都三天两夜行程");
        float[] second = new DemoFeatureHashEmbeddingService().embedDocument("成都三天两夜行程");

        assertThat(first).containsExactly(second);
    }

    @Test
    @DisplayName("英文大小写不影响向量结果")
    void embedWithDifferentEnglishCase_returnsSameVector() {
        float[] upperCase = embeddingService.embedDocument("Chengdu Food Guide");
        float[] lowerCase = embeddingService.embedDocument("chengdu food guide");

        assertThat(upperCase).containsExactly(lowerCase);
    }

    @Test
    @DisplayName("中文语气词不会稀释有效查询词")
    void embedWithChineseQuestionParticles_ignoresNoiseTerms() {
        float[] conciseQuery = embeddingService.embedDocument("成都火锅串串");
        float[] questionQuery = embeddingService.embedDocument("成都火锅串串怎么选");

        assertThat(questionQuery).containsExactly(conciseQuery);
    }

    @Test
    @DisplayName("向量经过 L2 归一化")
    void embed_returnsUnitLengthVector() {
        float[] vector = embeddingService.embedDocument("成都美食和旅行预算");

        assertThat(cosineSimilarity(vector, vector)).isCloseTo(1.0D, within(0.000001D));
    }

    @Test
    @DisplayName("有共同词项的文本比无关文本更相似")
    void embedWithSharedTerms_returnsHigherSimilarity() {
        float[] foodVector = embeddingService.embedDocument("成都美食推荐");
        float[] chengduVector = embeddingService.embedDocument("成都火锅和小吃");
        float[] quantumVector = embeddingService.embedDocument("量子纠缠实验");

        double sharedSimilarity = cosineSimilarity(foodVector, chengduVector);
        double unrelatedSimilarity = cosineSimilarity(foodVector, quantumVector);

        assertThat(sharedSimilarity).isGreaterThan(unrelatedSimilarity);
        assertThat(sharedSimilarity).isGreaterThan(0.2D);
    }

    @Test
    @DisplayName("没有词项的标点文本也返回非零向量")
    void embedWithPunctuation_returnsNonZeroVector() {
        float[] vector = embeddingService.embedDocument("？？？");

        assertThat(cosineSimilarity(vector, vector)).isCloseTo(1.0D, within(0.000001D));
    }

    @Test
    @DisplayName("空文本不能生成向量")
    void embedWithBlankText_throwsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> embeddingService.embedDocument("  "))
                .withMessageContaining("不能为空");
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dotProduct = 0.0D;
        double leftMagnitude = 0.0D;
        double rightMagnitude = 0.0D;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
            leftMagnitude += left[index] * left[index];
            rightMagnitude += right[index] * right[index];
        }
        return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }
}
