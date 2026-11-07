package com.aistudymentor.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTelemetryDto {

    private String modelId;
    private String promptVersion;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private boolean cacheHit;
}
