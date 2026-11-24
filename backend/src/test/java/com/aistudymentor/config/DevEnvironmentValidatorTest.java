package com.aistudymentor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevEnvironmentValidatorTest {

    private final DevEnvironmentValidator validator = new DevEnvironmentValidator();

    @Test
    void reportsAllMissingDevelopmentVariablesWithoutPrintingValues() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.postProcessEnvironment(environment, new SpringApplication()));

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
        assertTrue(exception.getMessage().contains("GEMINI_API_KEY"));
        assertTrue(exception.getMessage().contains("DB_URL"));
        assertTrue(exception.getMessage().contains("REDIS_PASSWORD"));
    }

    @Test
    void requiresExactlyThirtyTwoBytesForMfaEncryptionKey() {
        MockEnvironment environment = validDevEnvironment()
                .withProperty("MFA_ENCRYPTION_KEY", "too-short");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validator.postProcessEnvironment(environment, new SpringApplication()));

        assertTrue(exception.getMessage().contains("exactly 32 UTF-8 bytes"));
    }

    @Test
    void doesNotApplyDevelopmentRequirementsToOtherProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertDoesNotThrow(() -> validator.postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void acceptsCompleteDevelopmentConfiguration() {
        assertDoesNotThrow(() -> validator.postProcessEnvironment(validDevEnvironment(), new SpringApplication()));
    }

    @Test
    void allowsDevelopmentConfigurationWithoutGeminiCredentials() {
        MockEnvironment environment = validDevEnvironment();
        MapPropertySource properties = (MapPropertySource) environment.getPropertySources().get("mockProperties");
        properties.getSource().remove("GEMINI_API_KEY");
        properties.getSource().remove("GEMINI_MODEL");
        properties.getSource().remove("GEMINI_API_VERSION");

        assertDoesNotThrow(() -> validator.postProcessEnvironment(environment, new SpringApplication()));
    }

    private MockEnvironment validDevEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        return environment.withProperty("JWT_SECRET", "test-jwt-secret")
                .withProperty("MFA_ENCRYPTION_KEY", "01234567890123456789012345678901")
                .withProperty("GEMINI_API_KEY", "test-api-key")
                .withProperty("GEMINI_MODEL", "test-model")
                .withProperty("GEMINI_API_VERSION", "v1beta")
                .withProperty("DB_URL", "jdbc:mysql://localhost/test")
                .withProperty("DB_DRIVER", "com.mysql.cj.jdbc.Driver")
                .withProperty("DB_USERNAME", "test-user")
                .withProperty("DB_PASSWORD", "test-password")
                .withProperty("REDIS_HOST", "localhost")
                .withProperty("REDIS_PORT", "6379")
                .withProperty("REDIS_PASSWORD", "test-password")
                .withProperty("MANAGEMENT_USERNAME", "monitor")
                .withProperty("MANAGEMENT_PASSWORD", "a-local-test-management-password")
                .withProperty("DEV_SEED_PASSWORD", "test-password")
                .withProperty("DEV_SEED_TOTP_SECRET", "JBSWY3DPEHPK3PXP");
    }
}
