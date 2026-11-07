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
@Testcontainers
@ActiveProfiles("test")
public class FlywaySchemaValidationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("ai_study_mentor")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.jwt.secret", () -> "01234567890123456789012345678901_test_secret_32bytes_long");
        registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration-ms", () -> "604800000");
        registry.add("app.jwt.mfa-session-expiration-ms", () -> "300000");
        registry.add("app.security.mfa-encryption-key", () -> "01234567890123456789012345678901");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsAndFlywayValidatesSchema() {
        assertNotNull(applicationContext, "Spring application context must load successfully with Flyway + ddl-auto=validate");
    }
}
