package com.aistudymentor.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AiProviderProfileGuard implements ApplicationRunner {

    private final Environment environment;

    public AiProviderProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String provider = environment.getProperty("ai.provider", "gemini");
        if ("stub".equals(provider)
                && (environment.matchesProfiles("dev") || environment.matchesProfiles("prod"))) {
            throw new IllegalStateException(
                    "ai.provider=stub is only available in test profiles; configure ai.provider=gemini for dev and prod.");
        }
    }
}
