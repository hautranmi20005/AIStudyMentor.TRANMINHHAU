package com.aistudymentor.config;

import com.aistudymentor.common.CorrelationIds;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesSafeClientIdInMdcAndResponseThenClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profile/me");
        request.addHeader(CorrelationIds.HEADER_NAME, "client.trace-17");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertEquals("client.trace-17", MDC.get(CorrelationIds.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertEquals("client.trace-17", response.getHeader(CorrelationIds.HEADER_NAME));
        assertEquals("client.trace-17", request.getAttribute(CorrelationIds.REQUEST_ATTRIBUTE));
        assertNull(MDC.get(CorrelationIds.MDC_KEY));
    }

    @Test
    void replacesUnsafeClientIdWithGeneratedId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profile/me");
        request.addHeader(CorrelationIds.HEADER_NAME, "bad\r\n forged=true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        String correlationId = response.getHeader(CorrelationIds.HEADER_NAME);
        assertEquals(36, correlationId.length());
        assertEquals(correlationId, request.getAttribute(CorrelationIds.REQUEST_ATTRIBUTE));
    }
}
