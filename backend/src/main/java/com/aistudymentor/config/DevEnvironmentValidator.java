package com.aistudymentor.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DevEnvironmentValidator implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "JWT_SECRET",
            "MFA_ENCRYPTION_KEY",
            "DB_URL",
            "DB_DRIVER",
            "DB_USERNAME",
            "DB_PASSWORD",
            "REDIS_HOST",
            "REDIS_PORT",
            "REDIS_PASSWORD",
            "MANAGEMENT_USERNAME",
            "MANAGEMENT_PASSWORD",
            "DEV_SEED_PASSWORD",
            "DEV_SEED_TOTP_SECRET");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return;
        }

        List<String> missing = REQUIRED_VARIABLES.stream()
                .filter(name -> environment.getProperty(name, "").isBlank()
                        || environment.getProperty(name, "").startsWith("__GENERATED_BY_SCRIPT__"))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required development environment variables: "
                    + String.join(", ", missing)
                    + ". Copy backend/.env.example to backend/.env and generate local secrets. "
                    + "GEMINI_API_KEY is optional for backend startup but required for Ask AI.");
        }

        int encryptionKeyBytes = environment.getProperty("MFA_ENCRYPTION_KEY")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (encryptionKeyBytes != 32) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must be exactly 32 UTF-8 bytes for the local development profile.");
        }
    }
}
