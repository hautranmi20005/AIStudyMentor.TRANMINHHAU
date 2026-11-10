package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SuppressWarnings("null")
class GeminiAdapterServiceTest {

    private static final String MODEL_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStructuredExplanationFromOfficialGenerateContentResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(MODEL_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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

    private GeminiAdapterService adapter(RestClient.Builder builder) {
        return new GeminiAdapterService(
                builder,
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator(),
                "test-api-key",
                "gemini-2.5-flash",
                "v1beta");
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