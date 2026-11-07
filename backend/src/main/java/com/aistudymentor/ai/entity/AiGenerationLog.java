package com.aistudymentor.ai.entity;

import com.aistudymentor.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiGenerationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "quiz_id")
    private UUID quizId;

    @Column(name = "model_id", nullable = false, length = 100)
    private String modelId;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "cache_hit", nullable = false)
    @Builder.Default
    private Boolean cacheHit = false;
}
