package com.aistudymentor.question.service;

import com.aistudymentor.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ImageQuestionQuotaService {

    private final StringRedisTemplate redis;
    private final int maximumCallsPerDay;

    public ImageQuestionQuotaService(
            StringRedisTemplate redis,
            @Value("${app.ai.image.max-calls-per-day:10}") int maximumCallsPerDay) {
        if (maximumCallsPerDay < 1) {
            throw new IllegalArgumentException("app.ai.image.max-calls-per-day must be positive.");
        }
        this.redis = redis;
        this.maximumCallsPerDay = maximumCallsPerDay;
    }

    public void consume(UUID profileId) {
        LocalDate utcDate = LocalDate.now(ZoneOffset.UTC);
        String key = "studymentor:ai:image-quota:" + profileId + ":" + utcDate;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                throw unavailable();
            }
            if (count == 1) {
                Instant nextDay = utcDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                redis.expire(key, Duration.between(Instant.now(), nextDay));
            }
            if (count > maximumCallsPerDay) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI_IMAGE_RATE_LIMITED",
                        "The daily photo-question limit has been reached. Please try again tomorrow.");
            }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_QUOTA_UNAVAILABLE",
                "Photo questions are temporarily unavailable. Please try again shortly.");
    }
}
