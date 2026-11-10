package com.aistudymentor.config;

import com.aistudymentor.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-source/account, per-route fixed-window limits. Redis Lua provides atomic counters across
 * replicas; a bounded in-process fallback keeps a single instance protected if Redis is down.
 */
@Slf4j
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final DefaultRedisScript<Long> INCREMENT_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]); "
                    + "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return n",
            Long.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String keyPrefix;
    private final int loginLimit;
    private final Duration loginWindow;
    private final int registerLimit;
    private final Duration registerWindow;
    private final int refreshLimit;
    private final Duration refreshWindow;
    private final int mfaLimit;
    private final Duration mfaWindow;
    private final int aiLimit;
    private final Duration aiWindow;
    private final Map<String, WindowCounter> fallbackCounters = new ConcurrentHashMap<>();
    private final AtomicLong lastRedisWarningMillis = new AtomicLong();

    public ApiRateLimitFilter(
            StringRedisTemplate redisTemplate,
            JwtTokenProvider jwtTokenProvider,
            ObjectMapper objectMapper,
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.key-prefix:studymentor:rate-limit}") String keyPrefix,
            @Value("${app.security.rate-limit.login.limit:10}") int loginLimit,
            @Value("${app.security.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.security.rate-limit.register.limit:5}") int registerLimit,
            @Value("${app.security.rate-limit.register.window-seconds:3600}") long registerWindowSeconds,
            @Value("${app.security.rate-limit.refresh.limit:30}") int refreshLimit,
            @Value("${app.security.rate-limit.refresh.window-seconds:60}") long refreshWindowSeconds,
            @Value("${app.security.rate-limit.mfa.limit:5}") int mfaLimit,
            @Value("${app.security.rate-limit.mfa.window-seconds:900}") long mfaWindowSeconds,
            @Value("${app.security.rate-limit.ai.limit:20}") int aiLimit,
            @Value("${app.security.rate-limit.ai.window-seconds:60}") long aiWindowSeconds) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.keyPrefix = keyPrefix;
        this.loginLimit = loginLimit;
        this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
        this.registerLimit = registerLimit;
        this.registerWindow = Duration.ofSeconds(registerWindowSeconds);
        this.refreshLimit = refreshLimit;
        this.refreshWindow = Duration.ofSeconds(refreshWindowSeconds);
        this.mfaLimit = mfaLimit;
        this.mfaWindow = Duration.ofSeconds(mfaWindowSeconds);
        this.aiLimit = aiLimit;
        this.aiWindow = Duration.ofSeconds(aiWindowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !enabled || "OPTIONS".equalsIgnoreCase(request.getMethod()) || policyFor(request) == null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        RatePolicy policy = policyFor(request);
        String remoteAddress = request.getRemoteAddr();
        if (policy == null || !StringUtils.hasText(remoteAddress)) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = remoteAddress;
        if ("ai-generation".equals(policy.name)) {
            identity = authenticatedUserId(request).orElse(remoteAddress);
        }
        String key = keyPrefix + ":" + policy.name + ":" + hash(identity);
        if (consume(key, policy)) {
            response.setHeader("Retry-After", Long.toString(Math.max(1, policy.window.toSeconds())));
            writeRateLimitResponse(response, request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> authenticatedUserId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        String token = authorization.substring(7);
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            if ("ACCESS".equals(claims.get("tokenType", String.class))) {
                String userId = claims.get("userId", String.class);
                if (StringUtils.hasText(userId)) {
                    return java.util.Optional.of(userId);
                }
            }
        } catch (RuntimeException ignored) {
            // Invalid tokens remain rate-limited by source IP; authentication handles rejection.
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("null")
    private boolean consume(String key, RatePolicy policy) {
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.execute(
                        INCREMENT_WINDOW_SCRIPT,
                        List.of(key),
                        Long.toString(policy.window.toMillis()));
                if (count != null) {
                    return count > policy.limit;
                }
            } catch (Exception exception) {
                // Redis outages must not remove rate protection on this instance.
                long now = System.currentTimeMillis();
                long previous = lastRedisWarningMillis.get();
                if (now - previous >= 60_000 && lastRedisWarningMillis.compareAndSet(previous, now)) {
                    log.warn("Redis rate-limit counter unavailable; using local fallback ({})",
                            exception.getClass().getSimpleName());
                }
            }
        }
        return consumeLocally(key, policy);
    }

    private synchronized boolean consumeLocally(String key, RatePolicy policy) {
        long now = System.currentTimeMillis();
        if (fallbackCounters.size() > 4096) {
            fallbackCounters.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis
                    >= entry.getValue().window.toMillis());
        }
        WindowCounter counter = fallbackCounters.compute(key, (ignored, current) -> {
            if (current == null || now - current.windowStartMillis >= policy.window.toMillis()) {
                return new WindowCounter(now, 1, policy.window);
            }
            return new WindowCounter(current.windowStartMillis, current.count + 1, current.window);
        });
        return counter.count > policy.limit;
    }

    private RatePolicy policyFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(path)) {
            return new RatePolicy("login", loginLimit, loginWindow);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/register".equals(path)) {
            return new RatePolicy("register", registerLimit, registerWindow);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/refresh".equals(path)) {
            return new RatePolicy("refresh", refreshLimit, refreshWindow);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/mfa/verify".equals(path)) {
            return new RatePolicy("mfa", mfaLimit, mfaWindow);
        }
        if ("POST".equalsIgnoreCase(method)
                && ("/api/v1/questions".equals(path)
                || path.matches("/api/v1/questions/[^/]+/alternative-explanation"))) {
            return new RatePolicy("ai-generation", aiLimit, aiWindow);
        }
        return null;
    }

    private void writeRateLimitResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.builder()
                .status(TOO_MANY_REQUESTS)
                .error("TOO_MANY_REQUESTS")
                .code("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please wait before trying again.")
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record RatePolicy(String name, int limit, Duration window) { }

    private record WindowCounter(long windowStartMillis, int count, Duration window) { }
}
