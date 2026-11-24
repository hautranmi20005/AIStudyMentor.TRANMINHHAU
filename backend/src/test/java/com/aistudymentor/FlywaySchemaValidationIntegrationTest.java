package com.aistudymentor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FlywaySchemaValidationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("ai_study_mentor")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.jwt.secret", () -> "01234567890123456789012345678901_test_secret_32bytes_long");
        registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration-ms", () -> "604800000");
        registry.add("app.jwt.mfa-session-expiration-ms", () -> "300000");
        registry.add("app.security.mfa-encryption-key", () -> "01234567890123456789012345678901");
        registry.add("app.gemini.api-key", () -> "test_gemini_key");
        registry.add("app.gemini.model", () -> "gemini-3.8-flash");
        registry.add("app.gemini.api-version", () -> "v1beta");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("management.health.mail.enabled", () -> "false");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsAndFlywayValidatesSchema() {
        assertNotNull(applicationContext,
                "Spring application context must load successfully with Flyway + ddl-auto=validate");
    }
}
