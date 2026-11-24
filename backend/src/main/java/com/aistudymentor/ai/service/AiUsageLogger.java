package com.aistudymentor.ai.service;

import com.aistudymentor.ai.entity.AiGenerationLog;
import com.aistudymentor.ai.repository.AiGenerationLogRepository;
import com.aistudymentor.profile.entity.StudentProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiUsageLogger {

    private final AiGenerationLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiGenerationLog log(
            StudentProfile profile,
            UUID questionId,
            UUID quizId,
            String modelId,
            String promptVersion,
            Integer inputTokens,
            Integer outputTokens,
            long latencyMs,
            boolean cacheHit,
            String outcome) {
        return repository.save(AiGenerationLog.builder()
                .profile(profile)
                .questionId(questionId)
                .quizId(quizId)
                .modelId(modelId)
                .promptVersion(promptVersion)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .latencyTotalMs(latencyMs)
                .cacheHit(cacheHit)
                .outcome(outcome)
                .build());
    }
}
