package com.aistudymentor.config;

import com.aistudymentor.ai.metrics.AiMetrics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class MeteredCacheManager implements CacheManager, InitializingBean {

    private final CacheManager delegate;
    private final AiMetrics aiMetrics;
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();

    public MeteredCacheManager(CacheManager delegate, AiMetrics aiMetrics) {
        this.delegate = delegate;
        this.aiMetrics = aiMetrics;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (delegate instanceof InitializingBean initializingDelegate) {
            initializingDelegate.afterPropertiesSet();
        }
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache == null ? null : caches.computeIfAbsent(name, ignored -> new MeteredCache(cache, aiMetrics));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    private static final class MeteredCache implements Cache {

        private final Cache delegate;
        private final AiMetrics aiMetrics;

        private MeteredCache(Cache delegate, AiMetrics aiMetrics) {
            this.delegate = delegate;
            this.aiMetrics = aiMetrics;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            recordLookup(value != null);
            return value;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            recordLookup(value != null);
            return value;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper cached = delegate.get(key);
            if (cached != null) {
                aiMetrics.recordCacheHit();
                @SuppressWarnings("unchecked")
                T value = (T) cached.get();
                return value;
            }
            aiMetrics.recordCacheMiss();
            return delegate.get(key, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        private void recordLookup(boolean hit) {
            if (hit) {
                aiMetrics.recordCacheHit();
            } else {
                aiMetrics.recordCacheMiss();
            }
        }
    }
}
