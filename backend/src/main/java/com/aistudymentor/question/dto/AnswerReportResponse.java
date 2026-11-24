package com.aistudymentor.question.dto;

import com.aistudymentor.question.entity.AnswerReportReason;
import com.aistudymentor.question.entity.AnswerReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnswerReportResponse(
        UUID reportId,
        AnswerReportReason reason,
        AnswerReportStatus status,
        LocalDateTime createdAt) {
}
