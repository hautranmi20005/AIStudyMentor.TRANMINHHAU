package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.AiQuotaResponse;
import com.aistudymentor.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

@Slf4j
@Service
public class AiQuotaService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local calls = tonumber(redis.call('GET', KEYS[1]) or '0')
            local tokens = tonumber(redis.call('GET', KEYS[2]) or '0')
            local maxCalls = tonumber(ARGV[1])
            local maxTokens = tonumber(ARGV[2])
            local reserveTokens = tonumber(ARGV[3])
            if calls + 1 > maxCalls or tokens + reserveTokens > maxTokens then
                return 0
            end
            calls = redis.call('INCR', KEYS[1])
            tokens = redis.call('INCRBY', KEYS[2], reserveTokens)
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxCalls;
    private final long maxTokens;
    private final int tokenReservation;
    private final ConcurrentMap<String, LocalUsage> localFallback = new ConcurrentHashMap<>();

    public AiQuotaService(
            StringRedisTemplate redis,
            @Value("${app.ai.quota.max-calls-per-day:100}") int maxCalls,
            @Value("${app.ai.quota.max-tokens-per-day:100000}") long maxTokens,
            @Value("${app.ai.quota.reserved-tokens-per-call:8192}") int tokenReservation) {
        this.redis = redis;
        this.maxCalls = maxCalls;
        this.maxTokens = maxTokens;
        this.tokenReservation = tokenReservation;
    }

    public Reservation reserve(UUID profileId) {
        Keys keys = keys(profileId);
        long ttl = secondsUntilUtcMidnight();
        Long accepted;
        try {
            accepted = redis.execute(RESERVE_SCRIPT, List.of(keys.calls(), keys.tokens()),
                    Integer.toString(maxCalls), Long.toString(maxTokens),
                    Integer.toString(tokenReservation), Long.toString(ttl));
        } catch (DataAccessException exception) {
            log.warn("Redis AI quota unavailable; enforcing a process-local daily fallback");
            return reserveLocally(keys);
        }
        if (accepted == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "AI_QUOTA_UNAVAILABLE",
                    "AI usage limits are temporarily unavailable.");
        }
        if (!Long.valueOf(1).equals(accepted)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED",
                    "Your daily AI study limit has been reached. Please try again tomorrow.");
        }
        recordLocalReservation(keys, tokenReservation);
        return new Reservation(keys, tokenReservation, remaining(keys), false);
    }

    public void reconcile(Reservation reservation, long actualTokens) {
        long adjustment = Math.max(0, actualTokens) - reservation.reservedTokens();
        if (reservation.localFallback()) {
            adjustLocalTokens(reservation.keys(), adjustment);
            return;
        }
        try {
            if (adjustment != 0) {
                redis.opsForValue().increment(reservation.keys().tokens(), adjustment);
            }
        } catch (DataAccessException exception) {
            log.warn("Redis AI quota reconciliation unavailable; updating process-local fallback");
        }
        adjustLocalTokens(reservation.keys(), adjustment);
    }

    public AiQuotaResponse remaining(UUID profileId) {
        return remaining(keys(profileId));
    }

    private AiQuotaResponse remaining(Keys keys) {
        try {
            String callValue = redis.opsForValue().get(keys.calls());
            String tokenValue = redis.opsForValue().get(keys.tokens());
            return quotaResponse(parseUsage(callValue), parseUsage(tokenValue));
        } catch (DataAccessException exception) {
            log.warn("Redis AI quota lookup unavailable; reporting process-local fallback");
            LocalUsage usage = localFallback.get(localKey(keys));
            if (usage == null) {
                return quotaResponse(0, 0);
            }
            synchronized (usage) {
                return quotaResponse(usage.calls, usage.tokens);
            }
        }
    }

    private Reservation reserveLocally(Keys keys) {
        LocalUsage usage = localFallback.computeIfAbsent(localKey(keys), ignored -> new LocalUsage());
        synchronized (usage) {
            if (usage.calls + 1 > maxCalls || usage.tokens + tokenReservation > maxTokens) {
                throw dailyLimitReached();
            }
            usage.calls++;
            usage.tokens += tokenReservation;
            return new Reservation(keys, tokenReservation,
                    quotaResponse(usage.calls, usage.tokens), true);
        }
    }

    private void recordLocalReservation(Keys keys, int tokens) {
        LocalUsage usage = localFallback.computeIfAbsent(localKey(keys), ignored -> new LocalUsage());
        synchronized (usage) {
            usage.calls++;
            usage.tokens += tokens;
        }
    }

    private void adjustLocalTokens(Keys keys, long adjustment) {
        if (adjustment == 0) {
            return;
        }
        LocalUsage usage = localFallback.computeIfAbsent(localKey(keys), ignored -> new LocalUsage());
        synchronized (usage) {
            usage.tokens = Math.max(0, usage.tokens + adjustment);
        }
    }

    private String localKey(Keys keys) {
        return keys.calls();
    }

    private AiQuotaResponse quotaResponse(long calls, long tokens) {
        return AiQuotaResponse.builder()
                .remainingCallsToday(Math.max(0, maxCalls - calls))
                .remainingTokensToday(Math.max(0, maxTokens - tokens))
                .build();
    }

    private BusinessException dailyLimitReached() {
        return new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI_QUOTA_EXCEEDED",
                "Your daily AI study limit has been reached. Please try again tomorrow.");
    }

    private static final class LocalUsage {
        private long calls;
        private long tokens;
    }

    private long parseUsage(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("AI quota counter in Redis is invalid.", exception);
        }
    }

    private Keys keys(UUID profileId) {
        String prefix = "studymentor:ai:quota:" + profileId + ":" + LocalDate.now(ZoneOffset.UTC);
        return new Keys(prefix + ":calls", prefix + ":tokens");
    }

    private long secondsUntilUtcMidnight() {
        return Math.max(1, Duration.between(
                java.time.Instant.now(),
                LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)).toSeconds());
    }

    public record Keys(String calls, String tokens) {
    }

    public record Reservation(
            Keys keys, int reservedTokens, AiQuotaResponse quotaAfterReservation, boolean localFallback) {
    }
}
