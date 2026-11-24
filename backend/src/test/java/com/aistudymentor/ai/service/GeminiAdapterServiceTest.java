package com.aistudymentor.ai.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
import com.aistudymentor.ai.dto.AnswerRequest;
import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.QuizRequest;
import com.aistudymentor.ai.dto.StructuredAnswer;
import com.aistudymentor.common.exception.AiNotConfiguredException;
import com.aistudymentor.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SuppressWarnings("null")
class GeminiAdapterServiceTest {

    private static final String MODEL_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private static final String FALLBACK_MODEL_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
    private static final String SECOND_FALLBACK_MODEL_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent";
    private static final String FALLBACK_MODELS =
            "gemini-3.5-flash-lite,gemini-flash-lite-latest,gemini-flash-latest";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStructuredExplanationFromOfficialGenerateContentResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Solve x + 2 = 7")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseMimeType")))
                .andRespond(withSuccess(apiResponse(explanationJson()), MediaType.APPLICATION_JSON));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "Mathematics", "Solve x + 2 = 7", false);

        assertEquals("A clear summary of the solution.", explanation.getSummary());
        assertEquals("x = 5", explanation.getSteps().get(0).getFormula());
        assertEquals("x = 5", explanation.getFinalAnswer());
        assertEquals(List.of("Check by substitution."), explanation.getKeyTakeaways());
        server.verify();
    }

    @Test
    void parsesJsonResponseWithOctetStreamContentType() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(
                        apiResponse(explanationJson()), MediaType.APPLICATION_OCTET_STREAM));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "Mathematics", "Solve x + 2 = 7", false);

        assertEquals("x = 5", explanation.getFinalAnswer());
        server.verify();
    }

    @Test
    void logsSanitizedResponseDetailsWhenProviderJsonCannotBeParsed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withSuccess("{\"message\":\"API key test-api-key\"", MediaType.APPLICATION_OCTET_STREAM));
        Logger logger = (Logger) LoggerFactory.getLogger(GeminiAdapterService.class);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        try {
            BusinessException failure = assertThrows(BusinessException.class,
                    () -> adapter(builder).generateQuiz(
                            QuizRequest.builder().subject("History").numQuestions(1).build(),
                            new PromptTemplateService.Prompt("Tutor", "quiz/v1", "quiz/v1")));

            assertEquals("AI_INVALID_RESPONSE", failure.getCode());
            String logged = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertTrue(logged.contains("HTTP status 200"));
            assertTrue(logged.contains("Content-Type application/octet-stream"));
            assertTrue(logged.contains("API key [REDACTED]"));
            assertFalse(logged.contains("test-api-key"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        server.verify();
    }

    @Test
    void fallsBackToNextModelWhenPrimaryReturnsServiceUnavailable() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"High demand\"}}"));
        server.expect(requestTo(FALLBACK_MODEL_ENDPOINT))
                .andRespond(withSuccess(
                        apiResponse(explanationJson()), MediaType.APPLICATION_OCTET_STREAM));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "Mathematics", "Solve x + 2 = 7", false);

        assertEquals("x = 5", explanation.getFinalAnswer());
        server.verify();
    }

    @Test
    void fallsBackWhenErrorBodyReportsTransientUnavailable() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"Temporary outage\"}}"));
        server.expect(requestTo(FALLBACK_MODEL_ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"Still busy\"}}"));
        server.expect(requestTo(SECOND_FALLBACK_MODEL_ENDPOINT))
                .andRespond(withSuccess(
                        apiResponse(explanationJson()), MediaType.APPLICATION_JSON));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "Mathematics", "Solve x + 2 = 7", false);

        assertEquals("x = 5", explanation.getFinalAnswer());
        server.verify();
    }

    @Test
    void generatesContextualExplanationForHistoryQuestions() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String historyExplanation = objectMapper.writeValueAsString(Map.of(
                "summary", "The Industrial Revolution transformed production and society.",
                "steps", List.of(
                        Map.of("stepNumber", 1, "title", "Identify the change",
                                "explanation", "Factories and machines increased the scale of production."),
                        Map.of("stepNumber", 2, "title", "Trace the social effects",
                                "explanation", "Factory work encouraged migration to growing industrial cities.")),
                "finalAnswer", "Industrialization reshaped work, cities, and social relationships.",
                "keyTakeaways", List.of("Technological change also produced major social change.")));
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Subject: History")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "How did industrialization change everyday life?")))
                .andRespond(withSuccess(apiResponse(historyExplanation), MediaType.APPLICATION_JSON));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "History", "How did industrialization change everyday life?", false);

        assertEquals("The Industrial Revolution transformed production and society.", explanation.getSummary());
        assertEquals(2, explanation.getSteps().size());
        assertEquals("Industrialization reshaped work, cities, and social relationships.",
                explanation.getFinalAnswer());
        server.verify();
    }

    @Test
    void generatesContextualExplanationsForScienceAndLiteratureQuestions() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String scienceExplanation = objectMapper.writeValueAsString(Map.of(
                "summary", "Photosynthesis converts light energy into chemical energy in plants.",
                "steps", List.of(
                        Map.of("stepNumber", 1, "title", "Capture light",
                                "explanation", "Chlorophyll absorbs light energy in the chloroplasts."),
                        Map.of("stepNumber", 2, "title", "Store energy",
                                "explanation", "That energy helps convert carbon dioxide and water into glucose.")),
                "finalAnswer", "Plants use light to make glucose from carbon dioxide and water.",
                "keyTakeaways", List.of("Light energy is stored in the chemical bonds of glucose.")));
        String literatureExplanation = objectMapper.writeValueAsString(Map.of(
                "summary", "The green light symbolizes hope within the novel's themes of longing.",
                "steps", List.of(
                        Map.of("stepNumber", 1, "title", "Notice the recurring image",
                                "explanation", "The light appears whenever Gatsby thinks about his future."),
                        Map.of("stepNumber", 2, "title", "Connect image and theme",
                                "explanation", "Its distance reflects how his idealized dream remains out of reach.")),
                "finalAnswer", "The green light represents Gatsby's unreachable hopes and idealized future.",
                "keyTakeaways", List.of("Interpret symbols using how they recur in the text.")));

        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.contents[0].parts[0].text").value(
                                org.hamcrest.Matchers.containsString("Subject: Science")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.systemInstruction.parts[0].text").value(
                                org.hamcrest.Matchers.containsString("science, connect mechanisms to evidence")))
                .andRespond(withSuccess(apiResponse(scienceExplanation), MediaType.APPLICATION_JSON));
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.contents[0].parts[0].text").value(
                                org.hamcrest.Matchers.containsString("Subject: Literature")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.systemInstruction.parts[0].text").value(
                                org.hamcrest.Matchers.containsString("literature, discuss the text")))
                .andRespond(withSuccess(apiResponse(literatureExplanation), MediaType.APPLICATION_JSON));

        GeminiStructuredExplanation science = adapter(builder).generateExplanation(
                "Science", "How does photosynthesis store energy?", false);
        GeminiStructuredExplanation literature = adapter(builder).generateExplanation(
                "Literature", "What does the green light symbolize in The Great Gatsby?", false);

        assertEquals(2, science.getSteps().size());
        assertEquals("Plants use light to make glucose from carbon dioxide and water.", science.getFinalAnswer());
        assertEquals(2, literature.getSteps().size());
        assertEquals("The green light represents Gatsby's unreachable hopes and idealized future.",
                literature.getFinalAnswer());
        server.verify();
    }

    @Test
    void sendsStudentImageAsGeminiInlineData() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        byte[] imageBytes = "student-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encodedImage = java.util.Base64.getEncoder().encodeToString(imageBytes);

        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.contents[0].parts[1].inlineData.mimeType").value("image/png"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .jsonPath("$.contents[0].parts[1].inlineData.data").value(encodedImage))
                .andRespond(withSuccess(apiResponse(explanationJson()), MediaType.APPLICATION_JSON));

        adapter(builder).generateExplanation("Science", "What is shown?", imageBytes, "image/png", false);

        server.verify();
    }

    @Test
    void retriesOnceWhenGeminiReturnsMalformedJson() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withSuccess(apiResponse("not-json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("prior response was invalid")))
                .andRespond(withSuccess(apiResponse(explanationJson()), MediaType.APPLICATION_JSON));

        GeminiStructuredExplanation explanation = adapter(builder).generateExplanation(
                "Mathematics", "Solve x + 2 = 7", false);

        assertEquals("x = 5", explanation.getFinalAnswer());
        server.verify();
    }

    @Test
    void mapsGeminiSocketTimeoutToGatewayTimeout() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withException(new SocketTimeoutException("provider read timed out")));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> adapter(builder).generateExplanation("Mathematics", "Solve 1 + 1", false));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, failure.getStatus());
        assertEquals("AI_PROVIDER_TIMEOUT", failure.getCode());
        server.verify();
    }

    @Test
    void mapsGeminiStructuredSocketTimeoutToGatewayTimeout() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withException(new SocketTimeoutException("provider read timed out")));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> adapter(builder).generateQuiz(
                        QuizRequest.builder().subject("History").numQuestions(1).build(),
                        new PromptTemplateService.Prompt("Tutor", "quiz/v1", "quiz/v1")));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, failure.getStatus());
        assertEquals("AI_PROVIDER_TIMEOUT", failure.getCode());
        server.verify();
    }

    @Test
    void logsGeminiStatusAndSanitizedProviderErrorMessage() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"API key test-api-key is invalid.\"}}"));
        Logger logger = (Logger) LoggerFactory.getLogger(GeminiAdapterService.class);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        try {
            BusinessException failure = assertThrows(BusinessException.class,
                    () -> adapter(builder).generateQuiz(
                            QuizRequest.builder().subject("History").numQuestions(1).build(),
                            new PromptTemplateService.Prompt("Tutor", "quiz/v1", "quiz/v1")));

            assertEquals("AI_PROVIDER_ERROR", failure.getCode());
            String logged = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertTrue(logged.contains("HTTP status 400"));
            assertTrue(logged.contains("Content-Type application/json"));
            assertTrue(logged.contains("API key [REDACTED] is invalid."));
            assertFalse(logged.contains("test-api-key"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        server.verify();
    }

    @Test
    void missingGeminiKeyReturnsConfigurationErrorBeforeNetworkCall() {
        GeminiAdapterService unconfigured = new GeminiAdapterService(
                RestClient.builder(),
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator(),
                "",
                "gemini-3.8-flash",
                "v1beta",
                FALLBACK_MODELS);

        AiNotConfiguredException failure = assertThrows(AiNotConfiguredException.class,
                () -> unconfigured.generateExplanation("Mathematics", "Solve 1 + 1", false));

        assertTrue(failure.getMessage().contains("AI_NOT_CONFIGURED"));
    }

    @Test
    void mapsGeminiRateLimitToRetryableTooManyRequests() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> adapter(builder).generateExplanation("History", "Explain the causes of the French Revolution.", false));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, failure.getStatus());
        assertEquals("AI_RATE_LIMITED", failure.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("wait a moment"));
        server.verify();
    }

    @Test
    void generatesStructuredAnswerUsingVersionedPrompt() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("answer/v1 content")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("keyConcepts")))
                .andRespond(withSuccess(apiResponse(objectMapper.writeValueAsString(Map.of(
                        "summary", "A clear summary of the solution.",
                        "steps", List.of(Map.of(
                                "stepNumber", 1,
                                "title", "Isolate the variable",
                                "explanation", "Subtract two from both sides.")),
                        "finalAnswer", "x = 5",
                        "keyConcepts", List.of("Inverse operations"),
                        "commonMistakes", List.of("Forgetting to subtract from both sides."),
                        "followUpQuestions", List.of("How can you verify the result?")))),
                        MediaType.APPLICATION_JSON));

        AnswerRequest request = AnswerRequest.builder()
                .subject("Mathematics")
                .educationLevel("middle school")
                .questionText("Solve x + 2 = 7")
                .build();
        StructuredAnswer answer = adapter(builder).generateAnswer(
                request, new PromptTemplateService.Prompt("Tutor", "answer/v1 content", "answer/v1"));

        assertEquals("Mathematics", answer.getSubject());
        assertEquals("middle school", answer.getDifficulty());
        assertEquals("x = 5", answer.getFinalAnswer());
        assertEquals(List.of("Inverse operations"), answer.getKeyConcepts());
        assertEquals("gemini-3.8-flash", answer.getModelId());
        assertEquals("answer/v1", answer.getPromptVersion());
        server.verify();
    }

    @Test
    void generatesStructuredQuizUsingVersionedPrompt() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("quiz/v2 content")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("acceptedAnswers")))
                .andRespond(withSuccess(apiResponse(objectMapper.writeValueAsString(Map.of(
                        "questions", List.of(Map.of(
                                "type", "MCQ",
                                "topic", "Algebra",
                                "prompt", "What is x if x + 2 = 7?",
                                "options", List.of("3", "5", "7"),
                                "correctAnswer", "5",
                                "acceptedAnswers", List.of(),
                                "explanation", "Subtract two from both sides."))))),
                        MediaType.APPLICATION_JSON));

        QuizRequest request = QuizRequest.builder()
                .subject("Mathematics")
                .educationLevel("middle school")
                .numQuestions(1)
                .build();
        GeneratedQuiz quiz = adapter(builder).generateQuiz(
                request, new PromptTemplateService.Prompt("Tutor", "quiz/v2 content", "quiz/v2"));

        assertEquals(1, quiz.getQuestions().size());
        assertEquals("5", quiz.getQuestions().get(0).getCorrectAnswer());
        assertEquals("gemini-3.8-flash", quiz.getModelId());
        assertEquals("quiz/v2", quiz.getPromptVersion());
        server.verify();
    }

    private GeminiAdapterService adapter(RestClient.Builder builder) {
        return new GeminiAdapterService(
                builder,
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator(),
                "test-api-key",
                "gemini-3.8-flash",
                "v1beta",
                FALLBACK_MODELS);
    }

    private String apiResponse(String modelOutput) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", modelOutput)))))));
    }

    private String explanationJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "summary", "A clear summary of the solution.",
                "steps", List.of(Map.of(
                        "stepNumber", 1,
                        "title", "Isolate the variable",
                        "explanation", "Subtract two from both sides.",
                        "formula", "x = 5")),
                "finalAnswer", "x = 5",
                "keyTakeaways", List.of("Check by substitution.")));
    }
}