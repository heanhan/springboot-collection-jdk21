package com.example.agentscope.travel.rag;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 本地假 Embedding：用 SHA-256 特征哈希验证 pgvector 流程，不具备真实语义能力。 */
@Service
@Profile("demo-embedding")
public class DemoFeatureHashEmbeddingService implements TravelEmbeddingService {

    private static final int DIMENSION = 256;
    private static final String MODEL_NAME = "demo-feature-hash-v3";
    private static final int HASH_BUCKETS_PER_TOKEN = 2;
    private static final String[] NOISE_PHRASES = {
            "推荐", "当地", "怎么", "如何", "什么", "哪些", "哪个", "多少",
            "需要", "介绍", "一下", "一些", "两天", "三天"
    };
    private static final Pattern LATIN_WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern HAN_SEQUENCE = Pattern.compile("\\p{IsHan}+");
    private static final String NOISE_CHARACTERS = "的了呢吗吧啊呀和与及或是有在到去这那我你他她它们请为被把跟并怎么";

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public float[] embedDocument(String text) {
        return createEmbedding(text);
    }

    @Override
    public float[] embedQuery(String text) {
        return createEmbedding(text);
    }

    private float[] createEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Embedding 输入文本不能为空");
        }

        Set<String> tokens = extractTokens(text);
        if (tokens.isEmpty()) {
            // 标点或单个噪声字没有可用词项时，用原文生成稳定向量，避免出现零向量。
            tokens.add(text.trim().toLowerCase(Locale.ROOT));
        }

        MessageDigest digest = createDigest();
        float[] vector = new float[DIMENSION];
        for (String token : tokens) {
            addTokenToVector(vector, token, digest);
        }
        normalize(vector);
        return vector;
    }

    /**
     * 提取英文单词和中文二元词，不依赖额外中文分词组件。
     */
    private Set<String> extractTokens(String text) {
        String normalized = normalizeText(text);
        Set<String> tokens = new LinkedHashSet<String>();

        Matcher latinMatcher = LATIN_WORD.matcher(normalized);
        while (latinMatcher.find()) {
            String word = latinMatcher.group();
            if (word.length() > 1) {
                tokens.add(word);
            }
        }

        Matcher hanMatcher = HAN_SEQUENCE.matcher(normalized);
        while (hanMatcher.find()) {
            String sequence = hanMatcher.group();
            for (int index = 0; index < sequence.length() - 1; index++) {
                String bigram = sequence.substring(index, index + 2);
                if (!containsNoiseCharacter(bigram)) {
                    tokens.add(bigram);
                }
            }
        }
        return tokens;
    }

    /**
     * 每个词项写入两个哈希桶，减少不同词项偶然碰撞造成的影响。
     */
    private void addTokenToVector(float[] vector, String token, MessageDigest digest) {
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        ByteBuffer hashBuffer = ByteBuffer.wrap(hash);
        for (int index = 0; index < HASH_BUCKETS_PER_TOKEN; index++) {
            // 维度是 2 的幂，位运算等价于对维度取模。
            int bucket = hashBuffer.getInt() & (DIMENSION - 1);
            float sign = (hashBuffer.get() & 1) == 0 ? 1.0F : -1.0F;
            vector[bucket] += sign;
        }
    }

    /**
     * 做 L2 归一化，让 pgvector 的余弦距离可以直接比较不同长度的文本。
     */
    private void normalize(float[] vector) {
        double squaredMagnitude = 0.0D;
        for (float value : vector) {
            squaredMagnitude += value * value;
        }

        if (squaredMagnitude == 0.0D) {
            // 极端哈希碰撞可能互相抵消，至少保证返回非零向量。
            vector[0] = 1.0F;
            return;
        }

        float magnitude = (float) Math.sqrt(squaredMagnitude);
        for (int index = 0; index < vector.length; index++) {
            vector[index] = vector[index] / magnitude;
        }
    }

    /**
     * 去掉常见问句和行程约束中的泛化短语，避免它们稀释真正的主题词。
     */
    private String normalizeText(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String phrase : NOISE_PHRASES) {
            normalized = normalized.replace(phrase, " ");
        }
        return normalized;
    }

    private boolean containsNoiseCharacter(String term) {
        for (int index = 0; index < term.length(); index++) {
            if (NOISE_CHARACTERS.indexOf(term.charAt(index)) >= 0) {
                return true;
            }
        }
        return false;
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }
}
