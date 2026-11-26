package com.example.agentscope.travel.rag;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 使用 PostgreSQL pgvector 保存和查询知识向量，MySQL 仍保存知识正文。
 */
@Repository
public class TravelVectorRepository {

    private static final Logger log = LoggerFactory.getLogger(TravelVectorRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final String modelName;
    private final int dimension;

    public TravelVectorRepository(
            @Qualifier("travelVectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            TravelEmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.modelName = embeddingService.getModelName();
        this.dimension = embeddingService.getDimension();
        initializeSchema();
    }

    /**
     * 判断向量是否已按当前正文和模型生成，避免重复调用 Embedding。
     */
    public boolean isUpToDate(String chunkId, String contentHash) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM travel_knowledge_embedding "
                        + "WHERE chunk_id = ? AND content_hash = ? AND model_name = ?",
                Integer.class,
                chunkId,
                contentHash,
                modelName);
        return count != null && count > 0;
    }

    /**
     * 新增或更新一个知识向量，PostgreSQL 通过 ON CONFLICT 保证幂等。
     */
    public void upsert(String chunkId, String contentHash, float[] embedding) {
        if (embedding == null || embedding.length != dimension) {
            throw new IllegalArgumentException(
                    "Embedding 维度必须为 " + dimension + "，实际为 "
                            + (embedding == null ? "null" : embedding.length));
        }
        String sql = "INSERT INTO travel_knowledge_embedding "
                + "(chunk_id, embedding, model_name, content_hash, embedded_at) "
                + "VALUES (?, CAST(? AS vector), ?, ?, ?) "
                + "ON CONFLICT (chunk_id) DO UPDATE SET "
                + "embedding = EXCLUDED.embedding, "
                + "model_name = EXCLUDED.model_name, "
                + "content_hash = EXCLUDED.content_hash, "
                + "embedded_at = EXCLUDED.embedded_at";
        jdbcTemplate.update(
                sql,
                chunkId,
                createVectorParameter(embedding),
                modelName,
                contentHash,
                Timestamp.from(Instant.now()));
    }

    /**
     * 使用 <=> 余弦距离检索候选片段，距离越小越相似。
     */
    public List<VectorMatch> search(float[] queryEmbedding, int limit) {
        if (queryEmbedding == null || queryEmbedding.length != dimension) {
            throw new IllegalArgumentException(
                    "查询向量维度必须为 " + dimension + "，实际为 "
                            + (queryEmbedding == null ? "null" : queryEmbedding.length));
        }
        String sql = "SELECT chunk_id, embedding <=> CAST(? AS vector) AS distance "
                + "FROM travel_knowledge_embedding "
                + "WHERE model_name = ? "
                + "ORDER BY distance ASC, chunk_id ASC "
                + "LIMIT ?";
        return jdbcTemplate.query(
                sql,
                this::mapVectorMatch,
                createVectorParameter(queryEmbedding),
                modelName,
                limit);
    }

    private PGobject createVectorParameter(float[] embedding) {
        PGobject vector = new PGobject();
        vector.setType("vector");
        try {
            vector.setValue(Arrays.toString(embedding));
        } catch (SQLException exception) {
            throw new IllegalStateException("构造 pgvector 参数失败", exception);
        }
        return vector;
    }

    private VectorMatch mapVectorMatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VectorMatch(resultSet.getString("chunk_id"), resultSet.getDouble("distance"));
    }

    private void initializeSchema() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS travel_knowledge_embedding ("
                        + "chunk_id VARCHAR(128) PRIMARY KEY, "
                        + "embedding VECTOR(" + dimension + ") NOT NULL, "
                        + "model_name VARCHAR(128) NOT NULL, "
                        + "content_hash CHAR(64) NOT NULL, "
                        + "embedded_at TIMESTAMPTZ NOT NULL"
                        + ")");
        validateVectorDimension();
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_travel_knowledge_embedding_model "
                        + "ON travel_knowledge_embedding (model_name)");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_travel_knowledge_embedding_hnsw "
                        + "ON travel_knowledge_embedding USING hnsw (embedding vector_cosine_ops)");
    }

    /**
     * 维度变化时明确失败，避免应用自动删除或覆盖已经积累的向量数据。
     */
    private void validateVectorDimension() {
        Integer storedDimension = jdbcTemplate.queryForObject(
                "SELECT atttypmod FROM pg_attribute "
                        + "WHERE attrelid = 'travel_knowledge_embedding'::regclass "
                        + "AND attname = 'embedding' AND NOT attisdropped",
                Integer.class);
        if (storedDimension != null && storedDimension != dimension) {
            throw new IllegalStateException(
                    "travel_knowledge_embedding 现有向量维度为 " + storedDimension
                            + "，当前 Embedding 维度为 " + dimension
                            + "。请备份后手工删除并重建向量表，应用不会自动删除向量数据。");
        }
        log.info("Initialized pgvector table for model {} with dimension {}",
                modelName, dimension);
    }

    /**
     * 一条向量检索结果，distance 是余弦距离，范围通常为 0 到 2。
     */
    public static final class VectorMatch {

        private final String chunkId;
        private final double distance;

        public VectorMatch(String chunkId, double distance) {
            this.chunkId = chunkId;
            this.distance = distance;
        }

        public String getChunkId() {
            return chunkId;
        }

        public double getDistance() {
            return distance;
        }
    }
}
