package com.aistudymentor.common.exception;

import com.aistudymentor.common.CorrelationIds;
import com.aistudymentor.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void emitsOnlyTheStandardErrorContractForBusinessErrors() {
        MockHttpServletRequest request = request();

        var response = handler.handleBusinessException(
                new BusinessException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Question not found."),
                request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("QUESTION_NOT_FOUND", response.getBody().getCode());
        assertEquals("/api/v1/questions", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void mapsRateLimitErrorsToTheStandardStructuredResponse() {
        var response = handler.handleBusinessException(
                new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Slow down."),
                request());

        assertEquals(429, response.getStatusCode().value());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().getCode());
    }

    @Test
    void validationDetailsDoNotExposeRejectedSecretValues() throws Exception {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "password", "private-password", false,
                null, null, "must not be blank"));

        var response = handler.handleValidationExceptions(
                new MethodArgumentNotValidException(null, exception.getBindingResult()), request());
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(response.getBody());

        assertEquals("VALIDATION_FAILED", response.getBody().getCode());
        assertFalse(json.contains("private-password"));
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("\"path\":\"/api/v1/questions\""));
        assertNotNull(((ErrorResponse) response.getBody()).getTimestamp());
    }

    @Test
    void unexpectedErrorsDoNotExposeExceptionText() {
        var response = handler.handleGeneralException(new IllegalStateException("sensitive exception text"), request());

        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
    }

    @Test
    void unsupportedContentTypesReturnAClientErrorWithoutExceptionDetails() {
        var response = handler.handleUnsupportedMediaType(
                new HttpMediaTypeNotSupportedException(
                        MediaType.TEXT_PLAIN, java.util.List.of(MediaType.APPLICATION_JSON)), request());

        assertEquals(415, response.getStatusCode().value());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().getCode());
        assertEquals("The request content type is not supported.", response.getBody().getMessage());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/questions");
        request.setAttribute(CorrelationIds.REQUEST_ATTRIBUTE, "trace-123");
        return request;
    }
}
