package com.aistudymentor.config;

import com.aistudymentor.ai.metrics.AiMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteredCacheManagerTest {

    @Test
    void recordsCacheHitsAndMisses() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CacheManager manager = new MeteredCacheManager(
                new ConcurrentMapCacheManager("test-cache"), new AiMetrics(registry));
        var cache = manager.getCache("test-cache");

        cache.get("missing");
        cache.put("existing", "value");
        cache.get("existing");

        assertEquals(1.0, registry.get("cache.miss").counter().count());
        assertEquals(1.0, registry.get("cache.hit").counter().count());
    }

    @Test
    void initializesDelegateCacheManager() throws Exception {
        InitializingTestCacheManager delegate = new InitializingTestCacheManager();
        MeteredCacheManager manager = new MeteredCacheManager(delegate,
                new AiMetrics(new SimpleMeterRegistry()));

        manager.afterPropertiesSet();

        assertTrue(delegate.initialized);
    }

    private static final class InitializingTestCacheManager implements CacheManager, InitializingBean {

        private boolean initialized;

        @Override
        public void afterPropertiesSet() {
            initialized = true;
        }

        @Override
        public org.springframework.cache.Cache getCache(String name) {
            return null;
        }

        @Override
        public Collection<String> getCacheNames() {
            return List.of();
        }
    }
}
