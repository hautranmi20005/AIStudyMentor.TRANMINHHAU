package com.aistudymentor.ai.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AiMetrics {

    private final Counter aiCalls;
    private final Counter aiErrors;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter aiCacheHits;
    private final Counter aiCacheMisses;
    private final Counter aiCacheErrors;
    private final Counter inputTokens;
    private final Counter outputTokens;
    private final Timer aiLatency;
    private final double inputPricePerMillion;
    private final double outputPricePerMillion;

    public AiMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, 0.3, 2.5);
    }

    @Autowired
    public AiMetrics(
            MeterRegistry meterRegistry,
            @Value("${app.ai.cost.input-usd-per-million:0.3}") double inputPricePerMillion,
            @Value("${app.ai.cost.output-usd-per-million:2.5}") double outputPricePerMillion) {
        aiCalls = meterRegistry.counter("ai.calls");
        aiErrors = meterRegistry.counter("ai.errors");
        cacheHits = meterRegistry.counter("cache.hit");
        cacheMisses = meterRegistry.counter("cache.miss");
        aiCacheHits = meterRegistry.counter("ai.cache.hits");
        aiCacheMisses = meterRegistry.counter("ai.cache.misses");
        aiCacheErrors = meterRegistry.counter("ai.cache.errors");
        inputTokens = meterRegistry.counter("ai.tokens.in");
        outputTokens = meterRegistry.counter("ai.tokens.out");
        aiLatency = meterRegistry.timer("ai.latency");
        this.inputPricePerMillion = inputPricePerMillion;
        this.outputPricePerMillion = outputPricePerMillion;
    }

    public void recordAiCall() {
        aiCalls.increment();
    }

    public void recordAiError() {
        aiErrors.increment();
    }

    public void recordAiLatency(long elapsedNanos) {
        aiLatency.record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    public void recordTokenUsage(JsonNode response) {
        JsonNode usage = response == null ? null : response.get("usageMetadata");
        if (usage == null) {
            return;
        }
        incrementIfPositive(inputTokens, usage.path("promptTokenCount").asLong(0));
        incrementIfPositive(outputTokens, usage.path("candidatesTokenCount").asLong(0));
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public void recordAiCacheHit() {
        aiCacheHits.increment();
    }

    public void recordAiCacheMiss() {
        aiCacheMisses.increment();
    }

    public void recordAiCacheError() {
        aiCacheErrors.increment();
    }

    public AiCostReport costReport() {
        double hits = aiCacheHits.count();
        double misses = aiCacheMisses.count();
        double lookups = hits + misses;
        double calls = aiCalls.count();
        double input = inputTokens.count();
        double output = outputTokens.count();
        double estimatedSavedInput = calls == 0 ? 0 : input / calls * hits;
        double estimatedSavedOutput = calls == 0 ? 0 : output / calls * hits;
        return new AiCostReport(
                (long) calls,
                (long) hits,
                (long) misses,
                (long) aiCacheErrors.count(),
                lookups == 0 ? 0 : hits / lookups,
                Math.round(estimatedSavedInput + estimatedSavedOutput),
                cost(input, output),
                cost(estimatedSavedInput, estimatedSavedOutput));
    }

    private double cost(double input, double output) {
        return (input * inputPricePerMillion + output * outputPricePerMillion) / 1_000_000;
    }

    private void incrementIfPositive(Counter counter, long amount) {
        if (amount > 0) {
            counter.increment(amount);
        }
    }

    public record AiCostReport(
            long providerCalls,
            long cacheHits,
            long cacheMisses,
            long cacheErrors,
            double cacheHitRate,
            long estimatedTokensSaved,
            double estimatedCostUsd,
            double estimatedCostSavedUsd) {
    }
}
