package com.aistudymentor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitFilterTest {

    @Test
    void limitsLoginAttemptsByClientIpAndReturnsRetryAfter() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(
                null,
            null,
                new ObjectMapper().findAndRegisterModules(),
                true,
                "test:rate-limit",
                2, 60,
                5, 3600,
                10, 60,
                5, 900,
                20, 60);
        AtomicInteger forwardedRequests = new AtomicInteger();

        for (int attempt = 1; attempt <= 3; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.0.2.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (servletRequest, servletResponse) -> forwardedRequests.incrementAndGet();

            filter.doFilter(request, response, chain);
            if (attempt <= 2) {
                assertEquals(200, response.getStatus());
            } else {
                assertEquals(429, response.getStatus());
                assertEquals("60", response.getHeader("Retry-After"));
                assertTrue(response.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
            }
        }

        assertEquals(2, forwardedRequests.get());
    }
}
