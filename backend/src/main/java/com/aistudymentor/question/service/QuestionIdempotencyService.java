package com.aistudymentor.question.service;

import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.question.dto.QuestionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
public class QuestionIdempotencyService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(3);
    private static final long WAIT_MILLIS = 30_000;
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration resultTtl;

    public QuestionIdempotencyService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.privacy.idempotency-retention:24h}") Duration resultTtl) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.resultTtl = resultTtl;
    }

    public QuestionResponse execute(
            UUID userId,
            String idempotencyKey,
            Object request,
            Supplier<QuestionResponse> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }
        validateKey(idempotencyKey);
        String redisKey = "studymentor:questions:idempotency:v1:" + userId + ":" + sha256(idempotencyKey);
        String lockKey = redisKey + ":lock";
        String fingerprint = fingerprint(request);
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;

        while (true) {
            Entry existing = read(redisKey);
            if (existing != null) {
                return replay(existing, fingerprint);
            }

            String lockToken = UUID.randomUUID().toString();
            Boolean acquired;
            try {
                acquired = redis.opsForValue().setIfAbsent(lockKey, lockToken, LOCK_TTL);
            } catch (DataAccessException exception) {
                log.warn("Question idempotency storage unavailable");
                throw idempotencyUnavailable();
            }
            if (Boolean.TRUE.equals(acquired)) {
                return executeAsOwner(redisKey, lockKey, lockToken, fingerprint, action);
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "IDEMPOTENCY_IN_PROGRESS",
                        "A request with this idempotency key is still processing. Retry shortly.");
            }
            pause();
        }
    }

    private QuestionResponse executeAsOwner(
            String redisKey,
            String lockKey,
            String lockToken,
            String fingerprint,
            Supplier<QuestionResponse> action) {
        try {
            Entry raced = read(redisKey);
            if (raced != null) {
                return replay(raced, fingerprint);
            }
            try {
                QuestionResponse response = action.get();
                write(redisKey, new Entry(fingerprint, response, null, null, null, null));
                return response;
            } catch (BusinessException exception) {
                write(redisKey, new Entry(fingerprint, null, exception.getStatus().value(),
                        exception.getCode(), exception.getMessage(), exception.getDetails()));
                throw exception;
            } catch (RuntimeException exception) {
                log.error("Question submission failed unexpectedly; idempotent error response cached ({})",
                        exception.getClass().getSimpleName());
                BusinessException sanitized = new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR", "An unexpected error occurred.");
                write(redisKey, new Entry(fingerprint, null, sanitized.getStatus().value(),
                        sanitized.getCode(), sanitized.getMessage(), null));
                throw sanitized;
            }
        } finally {
            release(lockKey, lockToken);
        }
    }

    private Entry read(String key) {
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, Entry.class);
        } catch (JsonProcessingException exception) {
            log.warn("Discarding an unreadable question idempotency entry");
            try {
                redis.delete(key);
                return null;
            } catch (DataAccessException deleteException) {
                throw idempotencyUnavailable();
            }
        } catch (DataAccessException exception) {
            log.warn("Question idempotency storage unavailable");
            throw idempotencyUnavailable();
        }
    }

    private void write(String key, Entry entry) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(entry), resultTtl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Question idempotency result could not be serialized.", exception);
        } catch (DataAccessException exception) {
            log.warn("Question idempotency result could not be stored");
            throw idempotencyUnavailable();
        }
    }

    private QuestionResponse replay(Entry entry, String fingerprint) {
        if (!entry.fingerprint().equals(fingerprint)) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "This idempotency key was already used for a different request.");
        }
        if (entry.errorStatus() != null) {
            throw new BusinessException(HttpStatus.valueOf(entry.errorStatus()), entry.errorCode(),
                    entry.errorMessage(), entry.errorDetails());
        }
        return entry.response();
    }

    private void release(String key, String lockToken) {
        try {
            redis.execute(RELEASE_LOCK, List.of(key), lockToken);
        } catch (DataAccessException exception) {
            log.warn("Question idempotency lock release failed");
        }
    }

    private void validateKey(String key) {
        if (key.length() > 128 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must contain 1 to 128 safe characters.");
        }
    }

    private String fingerprint(Object request) {
        try {
            return sha256(objectMapper.writeValueAsBytes(request));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Question request could not be fingerprinted.", exception);
        }
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for idempotency keys.", exception);
        }
    }

    private void pause() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "The request was interrupted while waiting for an earlier submission.");
        }
    }

    private BusinessException idempotencyUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "IDEMPOTENCY_UNAVAILABLE",
                "Request retry protection is temporarily unavailable.");
    }

    private record Entry(
            String fingerprint,
            QuestionResponse response,
            Integer errorStatus,
            String errorCode,
            String errorMessage,
            java.util.Map<String, Object> errorDetails) {
    }
}
