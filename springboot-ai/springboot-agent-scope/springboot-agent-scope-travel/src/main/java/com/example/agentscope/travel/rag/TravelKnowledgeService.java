package com.example.agentscope.travel.rag;

import com.example.agentscope.travel.api.KnowledgeSearchRequest;
import com.example.agentscope.travel.api.KnowledgeSearchResponse;
import com.example.agentscope.travel.api.KnowledgeSearchResult;
import com.example.agentscope.travel.config.TravelRagProperties;
import com.example.agentscope.travel.config.TravelVectorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 旅游知识库加载与检索服务。
 */
@Service
public class TravelKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(TravelKnowledgeService.class);
    // 避免单个知识文件占满 Top-K，让不同主题的资料都有机会返回。
    private static final int MAX_CHUNKS_PER_SOURCE = 2;
    // 当前正式知识库只收录不依赖具体城市的通用知识。
    private static final String GLOBAL_SCOPE = "GLOBAL";

    private final TravelKnowledgeChunkRepository repository;
    private final TravelEmbeddingService embeddingService;
    private final TravelVectorRepository vectorRepository;
    private final int defaultTopK;
    private final double minSimilarity;
    private final int candidateLimit;

    public TravelKnowledgeService(
            TravelKnowledgeChunkRepository repository,
            TravelRagProperties properties,
            TravelVectorProperties vectorProperties,
            TravelEmbeddingService embeddingService,
            TravelVectorRepository vectorRepository) {
        this.repository = repository;
        this.defaultTopK = properties.getTopK();
        this.minSimilarity = vectorProperties.getMinSimilarity();
        this.candidateLimit = vectorProperties.getCandidateLimit();
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;

        List<TravelKnowledgeDocument> seedDocuments = loadDocuments(properties.getKnowledgePattern());
        synchronizeDatabase(repository, seedDocuments);
        List<TravelKnowledgeChunk> chunks = repository.findAllByOrderByIdAsc();
        if (chunks.isEmpty()) {
            throw new IllegalStateException(
                    "未加载到旅游知识，请检查 travel.rag.knowledge-pattern 配置和 MySQL 业务库。");
        }
        synchronizeVectors(chunks);
        log.info("Loaded {} travel knowledge chunks from MySQL table travel_knowledge_chunk", chunks.size());
    }

    /**
     * 启动时把 Markdown 初始化素材同步到 MySQL；内容变化时保留原创建时间并更新修改时间。
     */
    private void synchronizeDatabase(
            TravelKnowledgeChunkRepository repository,
            List<TravelKnowledgeDocument> seedDocuments) {
        LocalDateTime now = LocalDateTime.now();
        List<TravelKnowledgeChunk> chunksToSave = new ArrayList<TravelKnowledgeChunk>();

        for (TravelKnowledgeDocument document : seedDocuments) {
            TravelKnowledgeChunk existing = repository.findById(document.getId()).orElse(null);
            if (existing == null) {
                chunksToSave.add(new TravelKnowledgeChunk(
                        document.getId(),
                        document.getTitle(),
                        document.getSource(),
                        document.getScope(),
                        document.getContent(),
                        now,
                        now));
                continue;
            }

            boolean changed = !Objects.equals(existing.getTitle(), document.getTitle())
                    || !Objects.equals(existing.getSource(), document.getSource())
                    || !Objects.equals(existing.getScope(), document.getScope())
                    || !Objects.equals(existing.getContent(), document.getContent());
            if (!changed) {
                continue;
            }
            existing.setTitle(document.getTitle());
            existing.setSource(document.getSource());
            existing.setScope(document.getScope());
            existing.setContent(document.getContent());
            existing.setUpdatedAt(now);
            chunksToSave.add(existing);
        }

        if (!chunksToSave.isEmpty()) {
            repository.saveAll(chunksToSave);
        }
    }

    /**
     * 启动时把 MySQL 知识正文转换为向量，正文或 Embedding 模型未变化时跳过。
     */
    private void synchronizeVectors(List<TravelKnowledgeChunk> chunks) {
        int updatedCount = 0;
        for (TravelKnowledgeChunk chunk : chunks) {
            String vectorText = chunk.getTitle() + "\n" + chunk.getContent();
            String contentHash = sha256(vectorText);
            if (vectorRepository.isUpToDate(chunk.getId(), contentHash)) {
                continue;
            }
            float[] embedding = embeddingService.embedDocument(vectorText);
            vectorRepository.upsert(chunk.getId(), contentHash, embedding);
            updatedCount++;
        }
        log.info("Synchronized {} travel knowledge vectors to pgvector model {}",
                updatedCount, embeddingService.getModelName());
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                int unsignedValue = value & 0xFF;
                result.append(Character.forDigit(unsignedValue >>> 4, 16));
                result.append(Character.forDigit(unsignedValue & 0x0F, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }

    /**
     * 先把问题转换为向量，再从 pgvector 取候选，最后从 MySQL 读取对应正文。
     */
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
        float[] queryEmbedding = embeddingService.embedQuery(request.getQuery());
        List<TravelVectorRepository.VectorMatch> matches =
                vectorRepository.search(queryEmbedding, candidateLimit);
        if (matches.isEmpty()) {
            return new KnowledgeSearchResponse(request.getQuery(), topK, new ArrayList<KnowledgeSearchResult>());
        }

        List<String> candidateIds = new ArrayList<String>();
        for (TravelVectorRepository.VectorMatch match : matches) {
            candidateIds.add(match.getChunkId());
        }
        Map<String, TravelKnowledgeChunk> chunksById = new HashMap<String, TravelKnowledgeChunk>();
        for (TravelKnowledgeChunk chunk : repository.findAllById(candidateIds)) {
            chunksById.put(chunk.getId(), chunk);
        }

        List<KnowledgeSearchResult> results = new ArrayList<KnowledgeSearchResult>();
        Map<String, Integer> sourceCounts = new HashMap<String, Integer>();
        for (TravelVectorRepository.VectorMatch match : matches) {
            // pgvector 的 <=> 返回余弦距离，因此相似度等于 1 减距离。
            double similarity = 1.0D - match.getDistance();
            if (similarity < minSimilarity) {
                continue;
            }
            TravelKnowledgeChunk chunk = chunksById.get(match.getChunkId());
            if (chunk == null) {
                continue;
            }

            int sourceCount = sourceCounts.containsKey(chunk.getSource())
                    ? sourceCounts.get(chunk.getSource()) : 0;
            if (sourceCount >= MAX_CHUNKS_PER_SOURCE) {
                continue;
            }
            int score = (int) Math.round(Math.max(0.0D, Math.min(1.0D, similarity)) * 100.0D);
            results.add(new KnowledgeSearchResult(
                    chunk.getId(),
                    chunk.getTitle(),
                    chunk.getSource(),
                    score,
                    chunk.getContent()));
            sourceCounts.put(chunk.getSource(), sourceCount + 1);
            if (results.size() == topK) {
                break;
            }
        }

        return new KnowledgeSearchResponse(request.getQuery(), topK, results);
    }

    private List<TravelKnowledgeDocument> loadDocuments(String knowledgePattern) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources(knowledgePattern);
        } catch (IOException exception) {
            throw new IllegalStateException("读取旅游知识文件失败：" + knowledgePattern, exception);
        }

        List<Resource> sortedResources = new ArrayList<Resource>(Arrays.asList(resources));
        Collections.sort(sortedResources, new Comparator<Resource>() {
            @Override
            public int compare(Resource left, Resource right) {
                return safeFilename(left).compareTo(safeFilename(right));
            }
        });

        List<TravelKnowledgeDocument> result = new ArrayList<TravelKnowledgeDocument>();
        for (Resource resource : sortedResources) {
            String source = safeFilename(resource);
            String markdown = readResource(resource);
            result.addAll(parseMarkdown(source, markdown));
        }
        return result;
    }

    private String readResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取旅游知识文件失败：" + safeFilename(resource), exception);
        }
    }

    private List<TravelKnowledgeDocument> parseMarkdown(String source, String markdown) {
        String baseName = source.endsWith(".md") ? source.substring(0, source.length() - 3) : source;
        String documentTitle = baseName;
        String sectionTitle = "概述";
        StringBuilder paragraph = new StringBuilder();
        List<TravelKnowledgeDocument> chunks = new ArrayList<TravelKnowledgeDocument>();
        String[] lines = markdown.replace("\r\n", "\n").split("\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("# ")) {
                addChunk(baseName, documentTitle, sectionTitle, paragraph, chunks);
                documentTitle = line.substring(2).trim();
                continue;
            }
            if (line.startsWith("## ") || line.startsWith("### ")) {
                addChunk(baseName, documentTitle, sectionTitle, paragraph, chunks);
                sectionTitle = line.replaceFirst("^#+\\s*", "").trim();
                continue;
            }
            if (line.isEmpty()) {
                addChunk(baseName, documentTitle, sectionTitle, paragraph, chunks);
                continue;
            }
            paragraph.append(line).append("\n");
        }
        addChunk(baseName, documentTitle, sectionTitle, paragraph, chunks);
        return chunks;
    }

    private void addChunk(
            String baseName,
            String documentTitle,
            String sectionTitle,
            StringBuilder paragraph,
            List<TravelKnowledgeDocument> chunks) {
        if (paragraph.length() == 0) {
            return;
        }

        String content = sectionTitle + "\n" + paragraph.toString().trim();
        String title = documentTitle + " - " + sectionTitle;
        String id = baseName + "-" + (chunks.size() + 1);
        chunks.add(new TravelKnowledgeDocument(
                id, title, baseName + ".md", GLOBAL_SCOPE, content));
        paragraph.setLength(0);
    }

    private String safeFilename(Resource resource) {
        String filename = resource.getFilename();
        return filename == null ? resource.toString() : filename;
    }

}
