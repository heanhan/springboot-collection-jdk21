package com.example.agentscope.travel.rag;

import com.example.agentscope.travel.api.KnowledgeSearchRequest;
import com.example.agentscope.travel.api.KnowledgeSearchResponse;
import com.example.agentscope.travel.config.TravelRagProperties;
import com.example.agentscope.travel.config.TravelVectorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旅游知识检索服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TravelKnowledgeServiceTest {

    @Mock
    private TravelKnowledgeChunkRepository repository;

    @Mock
    private TravelVectorRepository vectorRepository;

    private TravelKnowledgeService travelKnowledgeService;
    private List<TravelKnowledgeChunk> storedChunks;
    private Map<String, String> storedVectorHashes;
    private Map<String, float[]> storedVectors;
    private DemoFeatureHashEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        storedChunks = new ArrayList<TravelKnowledgeChunk>();
        storedVectorHashes = new HashMap<String, String>();
        storedVectors = new HashMap<String, float[]>();
        embeddingService = new DemoFeatureHashEmbeddingService();
        when(vectorRepository.isUpToDate(anyString(), anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            String contentHash = invocation.getArgument(1);
            return contentHash.equals(storedVectorHashes.get(id));
        });
        doAnswer(invocation -> {
            storedVectorHashes.put(invocation.getArgument(0), invocation.getArgument(1));
            storedVectors.put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(vectorRepository).upsert(anyString(), anyString(), any(float[].class));
        lenient().when(vectorRepository.search(any(float[].class), anyInt()))
                .thenAnswer(invocation -> searchStoredVectors(invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(repository.findAllById(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            List<TravelKnowledgeChunk> result = new ArrayList<TravelKnowledgeChunk>();
            for (TravelKnowledgeChunk chunk : storedChunks) {
                if (ids.contains(chunk.getId())) {
                    result.add(chunk);
                }
            }
            return result;
        });
        when(repository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            for (TravelKnowledgeChunk chunk : storedChunks) {
                if (id.equals(chunk.getId())) {
                    return Optional.of(chunk);
                }
            }
            return Optional.empty();
        });
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<TravelKnowledgeChunk> chunks = invocation.getArgument(0);
            for (TravelKnowledgeChunk chunk : chunks) {
                storedChunks.removeIf(existing -> existing.getId().equals(chunk.getId()));
                storedChunks.add(chunk);
            }
            return chunks;
        });
        when(repository.findAllByOrderByIdAsc()).thenAnswer(invocation -> new ArrayList<TravelKnowledgeChunk>(storedChunks));

        travelKnowledgeService = createService();
    }

    @Test
    @DisplayName("启动时把 Markdown 知识同步到 MySQL 仓储")
    void startup_synchronizesMarkdownKnowledgeToRepository() {
        assertThat(storedChunks).hasSize(19);
        assertThat(storedVectorHashes).hasSize(19);
        assertThat(storedChunks)
                .extracting(TravelKnowledgeChunk::getSource)
                .contains("chengdu-food.md", "travel-budget.md", "travel-safety.md");
        assertThat(storedChunks).allSatisfy(chunk -> {
            assertThat(chunk.getCreatedAt()).isNotNull();
            assertThat(chunk.getUpdatedAt()).isNotNull();
            assertThat(chunk.getContent()).isNotBlank();
        });
    }

    @Test
    @DisplayName("重复启动且知识未变化时不重复保存")
    void restartWithUnchangedKnowledge_doesNotSaveAgain() {
        clearInvocations(repository, vectorRepository);

        createService();

        verify(repository, never()).saveAll(anyList());
        verify(vectorRepository, never()).upsert(anyString(), anyString(), any(float[].class));
        assertThat(storedChunks).hasSize(19);
        assertThat(storedVectorHashes).hasSize(19);
    }

    @Test
    @DisplayName("数据库知识内容变化时只更新对应片段并保留创建时间")
    void restartWithChangedKnowledge_updatesOnlyChangedChunk() {
        TravelKnowledgeChunk changedChunk = storedChunks.get(0);
        String changedId = changedChunk.getId();
        LocalDateTime createdAt = changedChunk.getCreatedAt();
        LocalDateTime previousUpdatedAt = changedChunk.getUpdatedAt().minusDays(1);
        changedChunk.setContent("已经被人工修改的旧内容");
        changedChunk.setUpdatedAt(previousUpdatedAt);
        storedVectorHashes.put(changedId, "stale-vector-hash");
        clearInvocations(repository, vectorRepository);

        createService();

        verify(repository).saveAll(argThat(chunks -> {
            List<TravelKnowledgeChunk> savedChunks = (List<TravelKnowledgeChunk>) chunks;
            return savedChunks.size() == 1 && changedId.equals(savedChunks.get(0).getId());
        }));
        verify(vectorRepository).upsert(eq(changedId), anyString(), any(float[].class));
        assertThat(storedChunks).hasSize(19);
        assertThat(changedChunk.getContent()).isNotEqualTo("已经被人工修改的旧内容");
        assertThat(changedChunk.getCreatedAt()).isEqualTo(createdAt);
        assertThat(changedChunk.getUpdatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    @DisplayName("成都美食问题优先命中美食知识")
    void searchChengduFood_returnsFoodKnowledgeFirst() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("成都美食推荐", 3);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getTotalMatches()).isGreaterThan(0);
        assertThat(response.getResults().get(0).getSource()).isEqualTo("chengdu-food.md");
    }

    @Test
    @DisplayName("自然口语美食问题优先命中美食知识")
    void searchNaturalFoodQuestion_returnsFoodKnowledgeFirst() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("去成都两天，推荐当地火锅和串串", 3);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getTotalMatches()).isGreaterThan(0);
        assertThat(response.getResults().get(0).getSource()).isEqualTo("chengdu-food.md");
    }

    @Test
    @DisplayName("预算问题命中预算知识")
    void searchBudget_returnsBudgetKnowledgeFirst() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("旅行预算怎么分配", 3);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getTotalMatches()).isGreaterThan(0);
        assertThat(response.getResults().get(0).getSource()).isEqualTo("travel-budget.md");
    }

    @Test
    @DisplayName("知识库未覆盖的问题返回空结果")
    void searchUnknownTopic_returnsEmptyResults() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("量子纠缠实验", 3);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getTotalMatches()).isZero();
        assertThat(response.getResults()).isEmpty();
    }

    @Test
    @DisplayName("Top-K 限制返回片段数量")
    void searchWithTopK_limitsResultCount() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("成都 美食 预算 安全", 2);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getTopK()).isEqualTo(2);
        assertThat(response.getTotalMatches()).isLessThanOrEqualTo(2);
        assertThat(response.getResults()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("复合问题不会让单一来源占满全部结果")
    void searchMixedQuestion_keepsSourceDiversity() {
        when(vectorRepository.search(any(float[].class), anyInt())).thenReturn(Arrays.asList(
                new TravelVectorRepository.VectorMatch("chengdu-food-1", 0.05D),
                new TravelVectorRepository.VectorMatch("chengdu-food-2", 0.10D),
                new TravelVectorRepository.VectorMatch("chengdu-itinerary-1", 0.20D),
                new TravelVectorRepository.VectorMatch("travel-budget-1", 0.30D)));
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("成都三天两夜美食推荐", 3);

        KnowledgeSearchResponse response = travelKnowledgeService.search(request);

        assertThat(response.getResults())
                .extracting(result -> result.getSource())
                .containsExactly("chengdu-food.md", "chengdu-food.md", "chengdu-itinerary.md");
        assertThat(response.getResults())
                .extracting(result -> result.getScore())
                .containsExactly(95, 90, 80);
    }

    private List<TravelVectorRepository.VectorMatch> searchStoredVectors(float[] query, int limit) {
        List<TravelVectorRepository.VectorMatch> matches = new ArrayList<TravelVectorRepository.VectorMatch>();
        for (Map.Entry<String, float[]> entry : storedVectors.entrySet()) {
            matches.add(new TravelVectorRepository.VectorMatch(
                    entry.getKey(), 1.0D - cosineSimilarity(query, entry.getValue())));
        }
        matches.sort(new Comparator<TravelVectorRepository.VectorMatch>() {
            @Override
            public int compare(
                    TravelVectorRepository.VectorMatch left,
                    TravelVectorRepository.VectorMatch right) {
                int distanceCompare = Double.compare(left.getDistance(), right.getDistance());
                if (distanceCompare != 0) {
                    return distanceCompare;
                }
                return left.getChunkId().compareTo(right.getChunkId());
            }
        });
        return matches.subList(0, Math.min(limit, matches.size()));
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dotProduct = 0.0D;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
        }
        return dotProduct;
    }

    private TravelKnowledgeService createService() {
        TravelRagProperties properties = new TravelRagProperties(
                "classpath*:knowledge/travel/*.md", 3);
        TravelVectorProperties vectorProperties = new TravelVectorProperties(
                "jdbc:postgresql://localhost:5432/postgres", "postgres", "test", 0.15D, 20);
        return new TravelKnowledgeService(
                repository,
                properties,
                vectorProperties,
                embeddingService,
                vectorRepository);
    }
}
