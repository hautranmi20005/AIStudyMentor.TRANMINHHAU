package com.aistudymentor.ai.service;

import com.aistudymentor.ai.entity.AiGenerationLog;

public record AiExecution<T>(T value, AiGenerationLog usageLog, long remainingCallsToday) {
}
