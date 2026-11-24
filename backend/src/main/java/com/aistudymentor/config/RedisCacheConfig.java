package com.aistudymentor.config;

import com.aistudymentor.ai.metrics.AiMetrics;
import com.aistudymentor.dashboard.dto.DashboardMetricsResponse;
import com.aistudymentor.profile.dto.StudentProfileDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.Map;

@Configuration
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, AiMetrics aiMetrics) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig();
        RedisCacheConfiguration dashboardMetrics = defaults.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(DashboardMetricsResponse.class)));
        RedisCacheConfiguration userProfile = defaults.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(StudentProfileDto.class)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "dashboard-metrics", dashboardMetrics,
                        "user-profile", userProfile))
                .build();
        return new MeteredCacheManager(redisCacheManager, aiMetrics);
    }
}
