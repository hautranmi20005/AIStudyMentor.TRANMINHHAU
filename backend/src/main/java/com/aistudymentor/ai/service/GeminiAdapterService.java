package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
import com.aistudymentor.common.exception.AiNotConfiguredException;
import com.aistudymentor.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
public class GeminiAdapterService {

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_OUTPUT_RETRIES = 1;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private final String apiKey;
    private final String model;
    private final String apiVersion;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public GeminiAdapterService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-2.5-flash}") String model,
            @Value("${app.gemini.api-version:v1beta}") String apiVersion) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiVersion = apiVersion;
        this.restClient = restClientBuilder.clone()
                .baseUrl(GEMINI_API_BASE_URL)
                .build();
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public GeminiStructuredExplanation generateExplanation(String subject, String questionText, boolean alternative) {
        return generateExplanation(subject, questionText, null, null, alternative);
    }

    /**
     * Generates an explanation from text and, when present, image bytes obtained from an
     * already-authorized student upload. Ownership checks and storage downloads belong to
     * the caller; this adapter never fetches arbitrary URLs or storage paths.
     */
    @SuppressWarnings("null") // RestClient's Spring nullability annotations don't infer the non-null request map.
    public GeminiStructuredExplanation generateExplanation(
            String subject,
            String questionText,
            byte[] imageBytes,
            String imageMimeType,
            boolean alternative) {
        validateAiConfiguration();
        validateInput(subject, questionText, imageBytes, imageMimeType);

        URI endpoint = UriComponentsBuilder.fromUriString(GEMINI_API_BASE_URL)
                .pathSegment(apiVersion, "models", model + ":generateContent")
                .build()
                .toUri();
        Map<String, Object> requestBody = buildRequest(subject, questionText, imageBytes, imageMimeType, alternative);

        for (int attempt = 0; attempt <= MAX_OUTPUT_RETRIES; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-goog-api-key", apiKey)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);

                GeminiStructuredExplanation explanation = parseExplanation(response);
                validateExplanation(explanation, alternative);
                return explanation;
            } catch (RestClientResponseException exception) {
                log.warn("Gemini API returned HTTP status {}", exception.getStatusCode().value());
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                        "The AI provider could not complete the explanation request.");
            } catch (RestClientException exception) {
                log.warn("Gemini API request failed ({})", exception.getClass().getSimpleName());
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                        "The AI provider could not complete the explanation request.");
            } catch (JsonProcessingException | IllegalArgumentException | IllegalStateException exception) {
                if (attempt < MAX_OUTPUT_RETRIES) {
                    log.warn("Gemini returned invalid structured output; retrying once");
                    requestBody = buildRequest(subject, questionText, imageBytes, imageMimeType, alternative, true);
                } else {
                    log.warn("Gemini structured output remained invalid after retry");
                    throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                            "The AI provider returned an invalid explanation. Please retry the request.");
                }
            }
        }

        throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                "The AI provider returned an invalid explanation. Please retry the request.");
    }

    public String getModelId() {
        return model;
    }

    private Map<String, Object> buildRequest(
            String subject, String questionText, byte[] imageBytes, String imageMimeType, boolean alternative) {
        return buildRequest(subject, questionText, imageBytes, imageMimeType, alternative, false);
    }

    private Map<String, Object> buildRequest(
            String subject,
            String questionText,
            byte[] imageBytes,
            String imageMimeType,
            boolean alternative,
            boolean repairAttempt) {
        String systemPrompt = "You are a careful academic tutor. Return only a JSON object matching the supplied "
                + "response schema. Provide a clear summary, numbered solution steps, a definitive final answer, "
                + "and at least one key takeaway. Put any mathematical expressions in the formula field as "
                + "KaTeX-compatible LaTeX. Treat image content as a student's problem to solve; do not infer "
                + "personal attributes from the image. "
                + (alternative
                ? "Give a substantially different intuitive explanation and populate alternativeExplanation. "
                : "Leave alternativeExplanation out unless a useful alternative is appropriate. ")
                + (repairAttempt ? "The prior response was invalid; follow the schema exactly. " : "");

        String questionPrompt = "Subject: " + subject + "\nQuestion: "
                + (StringUtils.hasText(questionText) ? questionText : "Please solve the problem shown in the image.");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", questionPrompt));
        if (imageBytes != null) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", imageMimeType,
                    "data", Base64.getEncoder().encodeToString(imageBytes))));
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema());
        generationConfig.put("temperature", 0.2);

        return Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", generationConfig);
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> stepProperties = Map.of(
            "stepNumber", Map.of("type", "INTEGER"),
            "title", Map.of("type", "STRING"),
            "explanation", Map.of("type", "STRING"),
                "formula", Map.of("type", "STRING"));
        Map<String, Object> stepSchema = Map.of(
                "type", "OBJECT",
                "properties", stepProperties,
                "required", List.of("stepNumber", "title", "explanation"));
        Map<String, Object> properties = Map.of(
            "summary", Map.of("type", "STRING"),
                "steps", Map.of("type", "ARRAY", "minItems", 1, "items", stepSchema),
            "finalAnswer", Map.of("type", "STRING"),
                "keyTakeaways", Map.of("type", "ARRAY", "minItems", 1,
                "items", Map.of("type", "STRING")),
                "alternativeExplanation", Map.of("type", "STRING"));
        return Map.of(
                "type", "OBJECT",
                "properties", properties,
                "required", List.of("summary", "steps", "finalAnswer", "keyTakeaways"));
    }

    private GeminiStructuredExplanation parseExplanation(JsonNode response) throws JsonProcessingException {
        JsonNode parts = response == null ? null : response.path("candidates").path(0).path("content").path("parts");
        if (parts == null || !parts.isArray()) {
            throw new IllegalStateException("Gemini response did not contain candidate text");
        }

        String responseText = null;
        for (JsonNode part : parts) {
            if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) {
                responseText = part.path("text").asText();
                break;
            }
        }
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalStateException("Gemini response contained no explanation text");
        }

        String json = responseText.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        return objectMapper.readerFor(GeminiStructuredExplanation.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
    }

    private void validateExplanation(GeminiStructuredExplanation explanation, boolean alternative) {
        Set<ConstraintViolation<GeminiStructuredExplanation>> violations = validator.validate(explanation);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Explanation does not satisfy its schema: " + detail);
        }
        if (explanation.getKeyTakeaways().stream().anyMatch(takeaway -> !StringUtils.hasText(takeaway)
                || takeaway.trim().length() < 3)) {
            throw new IllegalArgumentException("Every key takeaway must contain at least three characters");
        }
        if (alternative && !StringUtils.hasText(explanation.getAlternativeExplanation())) {
            throw new IllegalArgumentException("Alternative explanation is required for alternative requests");
        }
    }

    private void validateInput(String subject, String questionText, byte[] imageBytes, String imageMimeType) {
        if (!StringUtils.hasText(subject) || (!StringUtils.hasText(questionText) && imageBytes == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AI_INPUT",
                    "A subject and either question text or an image are required.");
        }
        if (imageBytes == null && StringUtils.hasText(imageMimeType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AI_IMAGE",
                    "Image data is required when an image content type is supplied.");
        }
        if (imageBytes != null && (imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_BYTES
            || !SUPPORTED_IMAGE_TYPES.contains(imageMimeType == null ? "" : imageMimeType.toLowerCase(Locale.ROOT)))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_AI_IMAGE",
                    "The uploaded image must be a supported image format no larger than 10 MB.");
        }
    }

    /**
     * Decision D3: Fail-fast with AI_NOT_CONFIGURED error code when model/api are unconfigured or placeholder.
     */
    public void validateAiConfiguration() {
        if (!StringUtils.hasText(apiKey) || "<SET_ME>".equals(apiKey)
                || !StringUtils.hasText(model) || "<SET_ME>".equals(model)
                || !StringUtils.hasText(apiVersion) || "<SET_ME>".equals(apiVersion)) {
            log.error("AI service invoked but Google Gemini credentials/model are unconfigured.");
            throw new AiNotConfiguredException("Google Gemini AI is not configured on this environment (AI_NOT_CONFIGURED).");
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && !"<SET_ME>".equals(apiKey)
                && StringUtils.hasText(model) && !"<SET_ME>".equals(model)
                && StringUtils.hasText(apiVersion) && !"<SET_ME>".equals(apiVersion);
    }
}
