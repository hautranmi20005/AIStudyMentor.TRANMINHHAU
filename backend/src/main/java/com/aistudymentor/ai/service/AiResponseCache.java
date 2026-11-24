package com.aistudymentor.ai.service;

import com.aistudymentor.ai.metrics.AiMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class AiResponseCache {

    private static final String SCHEMA_VERSION = "v1";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AiMetrics metrics;
    private final Duration ttl;
    private final String environment;

    public AiResponseCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            AiMetrics metrics,
            @Value("${app.ai.cache.ttl:7d}") Duration ttl,
            @Value("${app.ai.cache.environment:${spring.profiles.active:local}}") String environment) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.ttl = ttl;
        this.environment = environment;
    }

    public String key(
            String question,
            String subject,
            String educationLevel,
            String explanationStyle,
            String ageGroup,
            String answerLanguage,
            String taskType,
            String promptVersion,
            String modelId,
            String imageMimeType,
            byte[] imageBytes,
            int outputCount) {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("question", normalizeQuestion(question, subject));
        inputs.put("subject", normalize(subject));
        inputs.put("educationLevel", normalize(educationLevel));
        inputs.put("explanationStyle", normalize(explanationStyle));
        inputs.put("ageGroup", normalize(ageGroup));
        inputs.put("answerLanguage", normalize(answerLanguage));
        inputs.put("taskType", normalize(taskType));
        inputs.put("promptVersion", normalize(promptVersion));
        inputs.put("modelId", normalize(modelId));
        inputs.put("imageMimeType", normalize(imageMimeType));
        inputs.put("imageSha256", imageBytes == null ? "" : sha256(imageBytes));
        inputs.put("outputCount", Integer.toString(outputCount));
        try {
            String canonicalJson = objectMapper.writeValueAsString(inputs);
            return "studymentor:ai:" + normalize(environment) + ":" + SCHEMA_VERSION + ":"
                    + sha256(canonicalJson.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI cache key inputs could not be serialized.", exception);
        }
    }

    public String keyForUser(
            UUID profileId,
            String question,
            String subject,
            String educationLevel,
            String explanationStyle,
            String ageGroup,
            String answerLanguage,
            String taskType,
            String promptVersion,
            String modelId,
            String imageMimeType,
            byte[] imageBytes,
            int outputCount) {
        String contentKey = key(question, subject, educationLevel, explanationStyle, ageGroup,
                answerLanguage, taskType, promptVersion, modelId, imageMimeType, imageBytes, outputCount);
        return "studymentor:ai:user:" + profileId + ":" + contentKey.substring("studymentor:ai:".length());
    }

    public <T> T get(String key, Class<T> valueType) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            try {
                return objectMapper.readValue(json, valueType);
            } catch (JsonProcessingException exception) {
                metrics.recordAiCacheError();
                log.warn("Discarding an unreadable AI cache entry");
                try {
                    redis.delete(key);
                } catch (DataAccessException deleteException) {
                    metrics.recordAiCacheError();
                    log.warn("Could not remove an unreadable AI cache entry");
                }
                return null;
            }
        } catch (DataAccessException exception) {
            metrics.recordAiCacheError();
            log.warn("AI answer cache is unavailable; bypassing the cache");
            return null;
        }
    }

    public void put(String key, Object structuredResponse) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(structuredResponse), ttlWithJitter());
        } catch (JsonProcessingException exception) {
            metrics.recordAiCacheError();
            log.warn("AI response could not be serialized for caching");
        } catch (DataAccessException exception) {
            metrics.recordAiCacheError();
            log.warn("AI answer cache is unavailable; returning the provider response without caching");
        }
    }

    public void evict(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException exception) {
            metrics.recordAiCacheError();
            log.warn("AI cache entry could not be invalidated");
        }
    }

    private Duration ttlWithJitter() {
        long baseMillis = Math.max(1, ttl.toMillis());
        long jitterBound = baseMillis / 10;
        return Duration.ofMillis(baseMillis + (jitterBound == 0
                ? 0
                : ThreadLocalRandom.current().nextLong(jitterBound + 1)));
    }

    private String normalizeQuestion(String question, String subject) {
        String normalized = question == null ? "" : Normalizer.normalize(question, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ").trim();
        return "programming".equalsIgnoreCase(subject == null ? "" : subject.trim())
                ? normalized
                : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for AI cache keys.", exception);
        }
    }
}
