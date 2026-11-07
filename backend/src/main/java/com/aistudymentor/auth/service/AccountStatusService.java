package com.aistudymentor.auth.service;

import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.auth.entity.User;
import com.aistudymentor.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private static final String CACHE_KEY_PREFIX = "account:status:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final Optional<StringRedisTemplate> redisTemplate;

    /**
     * Loads account status strictly from DB/Cache (never purely trusting JWT claim).
     */
    public AccountStatus getAccountStatus(UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        // 1. Try Redis cache lookup
        if (redisTemplate.isPresent()) {
            try {
                String cached = redisTemplate.get().opsForValue().get(cacheKey);
                if (cached != null) {
                    return AccountStatus.valueOf(cached);
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable for account status check, falling back to DB: {}", ex.getMessage());
            }
        }

        // 2. Fetch directly from database
        AccountStatus status = userRepository.findById(userId)
                .map(User::getAccountStatus)
                .orElse(AccountStatus.SUSPENDED);

        // 3. Cache with short TTL (60s)
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(cacheKey, status.name(), CACHE_TTL);
            } catch (Exception ex) {
                log.warn("Failed to cache account status in Redis: {}", ex.getMessage());
            }
        }

        return status;
    }

    /**
     * Invalidate cache immediately when consent is approved, rejected, or account is suspended/activated.
     */
    public void invalidateCache(UUID userId) {
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().delete(CACHE_KEY_PREFIX + userId);
            } catch (Exception ex) {
                log.warn("Failed to invalidate account status in Redis: {}", ex.getMessage());
            }
        }
    }
}
