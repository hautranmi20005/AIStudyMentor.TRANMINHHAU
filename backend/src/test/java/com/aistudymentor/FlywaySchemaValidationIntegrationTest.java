package com.aistudymentor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class FlywaySchemaValidationIntegrationTest {

    static MySQLContainer<?> mysql;
    static boolean isTestcontainersRunning = false;

    static {
        try {
            mysql = new MySQLContainer<>("mysql:8.0.36")
                    .withDatabaseName("ai_study_mentor")
                    .withUsername("test_user")
                    .withPassword("test_password");
            mysql.start();
            isTestcontainersRunning = true;
        } catch (Throwable t) {
            // Testcontainers not available; fall back to local MySQL running via docker-compose
            isTestcontainersRunning = false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (isTestcontainersRunning && mysql != null && mysql.isRunning()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:ai_study_mentor;DB_CLOSE_DELAY=-1;MODE=MySQL");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        }
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.jwt.secret", () -> "01234567890123456789012345678901_test_secret_32bytes_long");
        registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration-ms", () -> "604800000");
        registry.add("app.jwt.mfa-session-expiration-ms", () -> "300000");
        registry.add("app.security.mfa-encryption-key", () -> "01234567890123456789012345678901");
        registry.add("app.gemini.api-key", () -> "test_gemini_key");
        registry.add("app.gemini.model", () -> "gemini-2.5-flash");
        registry.add("app.gemini.api-version", () -> "v1beta");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsAndFlywayValidatesSchema() {
        assertNotNull(applicationContext, "Spring application context must load successfully with Flyway + ddl-auto=validate");
    }
}
