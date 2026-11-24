package com.aistudymentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderProfileGuardTest {

    @Test
    void stubProviderIsRejectedInDev() {
        MockEnvironment environment = environmentWith("dev");

        assertThrows(IllegalStateException.class, () -> new AiProviderProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void stubProviderIsRejectedInProduction() {
        MockEnvironment environment = environmentWith("prod");

        assertThrows(IllegalStateException.class, () -> new AiProviderProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void stubProviderRemainsAvailableForTestProfiles() {
        MockEnvironment environment = environmentWith("test");

        assertDoesNotThrow(() -> new AiProviderProfileGuard(environment)
                .run(new DefaultApplicationArguments(new String[0])));
    }

    private MockEnvironment environmentWith(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        environment.setProperty("ai.provider", "stub");
        return environment;
    }
}
