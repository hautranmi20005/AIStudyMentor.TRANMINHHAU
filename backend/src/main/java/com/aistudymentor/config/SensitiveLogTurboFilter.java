package com.aistudymentor.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.regex.Pattern;

public class SensitiveLogTurboFilter extends TurboFilter {

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
            Pattern.compile("\\bAIza[A-Za-z0-9_-]{30,}\\b"),
            Pattern.compile("(?i)\\b(password|passwd|refresh[_-]?token|access[_-]?token|mfa[_-]?session[_-]?token|"
                    + "authorization|gemini[_-]?api[_-]?key|api[_-]?key|totp[_-]?code|verification[_-]?code)\\s*[:=]\\s*['\"]?[^\\s,;'\"]+"),
            Pattern.compile("(?i)\\b(question[_-]?text|image[_-]?bytes|inline[_-]?data)\\s*[:=]"),
            Pattern.compile("(?i)\\b(data:image/[^;]+;base64,)[A-Za-z0-9+/]{32,}={0,2}"),
            Pattern.compile("(?<![A-Za-z0-9+/])[A-Za-z0-9+/]{512,}={0,2}(?![A-Za-z0-9+/])")
    );

    @Override
    public FilterReply decide(
            Marker marker, Logger logger, Level level, String format, Object[] parameters, Throwable throwable) {
        if (format == null) {
            return FilterReply.NEUTRAL;
        }
        String rendered = MessageFormatter.arrayFormat(format, parameters).getMessage();
        if (rendered == null) {
            return FilterReply.NEUTRAL;
        }
        return SENSITIVE_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(rendered).find())
                ? FilterReply.DENY
                : FilterReply.NEUTRAL;
    }
}
