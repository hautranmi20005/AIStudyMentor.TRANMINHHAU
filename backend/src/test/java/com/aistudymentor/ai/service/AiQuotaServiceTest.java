package com.aistudymentor.ai.service;

import com.aistudymentor.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiQuotaServiceTest {

    @Test
    void acceptsReservationAndReportsRemainingQuota() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(null);
        AiQuotaService service = new AiQuotaService(redis, 3, 100, 10);
        UUID profileId = UUID.randomUUID();

        AiQuotaService.Reservation reservation = service.reserve(profileId);

        assertFalse(reservation.localFallback());
        assertEquals(10, reservation.reservedTokens());
        assertEquals(3L, reservation.quotaAfterReservation().getRemainingCallsToday());
        verify(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void rejectsReservationWhenRedisDailyLimitIsReached() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        AiQuotaService service = new AiQuotaService(redis, 1, 100, 10);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.reserve(UUID.randomUUID()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        assertEquals("AI_RATE_LIMITED", exception.getCode());
    }
}
