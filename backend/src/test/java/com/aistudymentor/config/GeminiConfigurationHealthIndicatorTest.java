package com.aistudymentor.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiConfigurationHealthIndicatorTest {

    @Test
    void reportsConfigurationWithoutCallingProvider() {
        var indicator = new GeminiConfigurationHealthIndicator("secret-key", "model-id", "v1beta");

        assertEquals("UP", indicator.health().getStatus().getCode());
        assertEquals(true, indicator.health().getDetails().get("configured"));
    }

    @Test
    void reportsUnconfiguredWithoutMarkingApplicationHealthDown() {
        var indicator = new GeminiConfigurationHealthIndicator("", "", "");

        assertEquals("UP", indicator.health().getStatus().getCode());
        assertEquals(false, indicator.health().getDetails().get("configured"));
    }
}
