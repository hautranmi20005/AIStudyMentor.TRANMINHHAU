package com.aistudymentor.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveLogTurboFilterTest {

    @Test
    void deniesLogLinesContainingCredentialsTokensOtpPromptsOrImagePayloads() {
        LoggerContext context = new LoggerContext();
        SensitiveLogTurboFilter filter = new SensitiveLogTurboFilter();
        filter.setContext(context);
        filter.start();
        context.addTurboFilter(filter);
        Logger logger = context.getLogger("redaction-test");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);

        String jwt = "eyJ" + "hbGciOiJIUzI1NiJ9" + "." + "eyJ1c2VySWQiOiIxMjM0NTYifQ"
                + "." + "signaturevalue123456";
        String geminiKey = "AIza" + "123456789012345678901234567890123456";
        logger.info("password={}", "do-not-log-this");
        logger.info("Authorization: Bearer {}", jwt);
        logger.info("GEMINI_API_KEY={}", geminiKey);
        logger.info("totp_code={}", "123456");
        logger.info("questionText={}", "private student prompt");
        logger.info("imageBytes={}", "private image payload");
        logger.info("image={}", "data:image/png;base64,ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        logger.info("Request completed with status={}", 200);

        assertEquals(1, appender.list.size());
        assertEquals("Request completed with status=200", appender.list.get(0).getFormattedMessage());
        assertFalse(appender.list.get(0).getFormattedMessage().contains("do-not-log-this"));
        context.stop();
    }
}
