package com.aistudymentor;

import com.aistudymentor.ai.dto.ExplanationStep;
import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
import com.aistudymentor.ai.service.GeminiAdapterService;
import com.aistudymentor.auth.entity.AccountStatus;
import com.aistudymentor.config.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level integration coverage for the main student workflows. Persistence and Flyway run
 * against a disposable MySQL Testcontainer; external mail and Gemini providers are mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings({"null", "resource"})
class BackendEndToEndIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("ai_study_mentor")
            .withUsername("integration_user")
            .withPassword("integration_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.jwt.secret", () -> "integration_test_secret_0123456789_abcdef");
        registry.add("app.jwt.access-token-expiration-ms", () -> "900000");
        registry.add("app.jwt.refresh-token-expiration-ms", () -> "604800000");
        registry.add("app.jwt.mfa-session-expiration-ms", () -> "300000");
        registry.add("app.security.mfa-encryption-key", () -> "01234567890123456789012345678901");
        registry.add("app.gemini.api-key", () -> "integration-test-key");
        registry.add("app.gemini.model", () -> "gemini-2.5-flash");
        registry.add("app.gemini.api-version", () -> "v1beta");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("app.mail.from", () -> "test@example.test");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private GeminiAdapterService geminiAdapterService;

    @BeforeEach
    void configureExternalAdapters() {
        when(mailSender.createMimeMessage()).thenAnswer(
                ignored -> new MimeMessage(Session.getInstance(new Properties())));
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void adultRegistrationLoginAndRefreshIssueUsableRotatedTokens() throws Exception {
        String email = uniqueEmail("adult");
        JsonNode registered = register(email, LocalDate.now().minusYears(20), null);
        assertEquals(HttpStatus.CREATED.value(), registered.path("httpStatus").asInt());
        assertEquals("ACTIVE", registered.path("accountStatus").asText());

        JsonNode login = login(email);
        assertTrue(!login.path("mfaRequired").asBoolean());
        assertTrue(login.path("accessToken").asText().length() > 20);
        String originalRefreshToken = login.path("refreshToken").asText();

        ResponseEntity<JsonNode> refreshedResponse = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                Map.of("refreshToken", originalRefreshToken),
                JsonNode.class);
        assertEquals(HttpStatus.OK, refreshedResponse.getStatusCode());
        JsonNode refreshed = refreshedResponse.getBody();
        assertNotNull(refreshed);
        assertTrue(refreshed.path("accessToken").isTextual());
        assertTrue(refreshed.path("refreshToken").isTextual());
        assertTrue(!originalRefreshToken.equals(refreshed.path("refreshToken").asText()));

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.fromString(registered.path("userId").asText()))
                .email(email)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk());
    }

    @Test
    void underThirteenRegistrationRequiresGuardianConsentAndBlocksStudyRoutes() throws Exception {
        String email = uniqueEmail("minor");
        JsonNode registered = register(email, LocalDate.now().minusYears(12), "guardian@example.test");
        assertEquals(HttpStatus.CREATED.value(), registered.path("httpStatus").asInt());
        assertEquals("PENDING_CONSENT", registered.path("accountStatus").asText());
        assertTrue(registered.path("guardianConsentRequired").asBoolean());
        assertEquals("PENDING", registered.path("consentStatus").asText());
        verify(mailSender).send(any(MimeMessage.class));

        JsonNode login = login(email);
        assertTrue(!login.path("mfaRequired").asBoolean());
        String accessToken = login.path("accessToken").asText();

        ResponseEntity<JsonNode> questionsResponse = restTemplate.exchange(
                url("/api/v1/questions"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(accessToken)),
                JsonNode.class);
        assertEquals(HttpStatus.FORBIDDEN, questionsResponse.getStatusCode());
        assertEquals("ACCOUNT_PENDING_CONSENT", questionsResponse.getBody().path("code").asText());
    }

    @Test
    void authenticatedQuestionSubmissionPersistsStructuredAiExplanation() throws Exception {
        String email = uniqueEmail("question");
        register(email, LocalDate.now().minusYears(20), null);
        JsonNode login = login(email);
        assertTrue(!login.path("mfaRequired").asBoolean());
        String token = login.path("accessToken").asText();
        when(geminiAdapterService.isConfigured()).thenReturn(true);
        when(geminiAdapterService.getModelId()).thenReturn("gemini-2.5-flash");
        when(geminiAdapterService.generateExplanation(anyString(), any(), eq(false))).thenReturn(
                GeminiStructuredExplanation.builder()
                        .summary("Subtract two from both sides to isolate x.")
                        .steps(List.of(new ExplanationStep(1, "Isolate x", "Subtract two from each side.", "x = 5")))
                        .finalAnswer("x = 5")
                        .keyTakeaways(List.of("Use inverse operations to solve equations."))
                        .build());

        ResponseEntity<JsonNode> submitted = restTemplate.exchange(
                url("/api/v1/questions"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("subject", "Mathematics", "questionText", "Solve x + 2 = 7"), bearerHeaders(token)),
                JsonNode.class);

        assertEquals(HttpStatus.CREATED, submitted.getStatusCode());
        JsonNode question = submitted.getBody();
        assertNotNull(question);
        String questionId = question.path("questionId").asText();
        assertEquals("ANSWERED", question.path("status").asText());
        assertEquals("Subtract two from both sides to isolate x.",
                question.path("answer").path("structuredExplanation").path("summary").asText());
        assertEquals("x = 5", question.path("answer").path("structuredExplanation")
                .path("steps").get(0).path("formula").asText());
        verify(geminiAdapterService).generateExplanation("Mathematics", "Solve x + 2 = 7", false);

        ResponseEntity<JsonNode> retrieved = restTemplate.exchange(
                url("/api/v1/questions/" + questionId),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, retrieved.getStatusCode());
        assertEquals(questionId, retrieved.getBody().path("questionId").asText());
        assertEquals("x = 5", retrieved.getBody().path("answer").path("structuredExplanation")
                .path("finalAnswer").asText());
    }

    @Test
    void quizCanBeGeneratedAttemptedAndGradedThroughAuthenticatedEndpoints() throws Exception {
        String email = uniqueEmail("quiz");
        register(email, LocalDate.now().minusYears(20), null);
        JsonNode login = login(email);
        assertTrue(!login.path("mfaRequired").asBoolean());
        String token = login.path("accessToken").asText();

        ResponseEntity<JsonNode> quizResponse = restTemplate.exchange(
                url("/api/v1/quizzes"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("subject", "Mathematics", "numQuestions", 2), bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, quizResponse.getStatusCode());
        JsonNode quiz = quizResponse.getBody();
        assertNotNull(quiz);
        String quizId = quiz.path("quizId").asText();
                List<Map<String, String>> quizResponses = new java.util.ArrayList<>();
                for (JsonNode quizQuestion : quiz.path("questions")) {
                        String optionBId = null;
                        for (JsonNode option : quizQuestion.path("options")) {
                                if ("Option B".equals(option.path("optionText").asText())) {
                                        optionBId = option.path("optionId").asText();
                                        break;
                                }
            }
                        assertNotNull(optionBId);
                        quizResponses.add(Map.of(
                                        "questionId", quizQuestion.path("questionId").asText(),
                                        "selectedOptionId", optionBId));
        }

        ResponseEntity<JsonNode> started = restTemplate.exchange(
                url("/api/v1/quizzes/" + quizId + "/attempts"),
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, started.getStatusCode());
        String attemptId = started.getBody().path("attemptId").asText();

        ResponseEntity<JsonNode> submitted = restTemplate.exchange(
                url("/api/v1/quizzes/" + quizId + "/attempts/" + attemptId + "/submit"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("responses", quizResponses), bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, submitted.getStatusCode());
        assertEquals(100, submitted.getBody().path("score").asInt());
        assertEquals(2, submitted.getBody().path("correctCount").asInt());
        assertEquals(2, submitted.getBody().path("totalQuestions").asInt());

        ResponseEntity<JsonNode> attempts = restTemplate.exchange(
                url("/api/v1/quizzes/attempts"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, attempts.getStatusCode());
        assertEquals(attemptId, attempts.getBody().get(0).path("attemptId").asText());
    }

    private JsonNode register(String email, LocalDate dob, String guardianEmail) throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("email", email);
        request.put("password", "Strong-password-123!");
        request.put("dob", dob.toString());
        request.put("educationLevel", "high-school");
        request.put("guardianEmail", guardianEmail);
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(url("/api/v1/auth/register"), request, JsonNode.class);
        JsonNode responseBody = response.getBody();
        JsonNode body = responseBody == null ? objectMapper.createObjectNode() : responseBody.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("httpStatus", response.getStatusCode().value());
        return body;
    }

    private JsonNode login(String email) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                Map.of("email", email, "password", "Strong-password-123!"),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
