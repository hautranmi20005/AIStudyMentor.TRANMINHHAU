package com.aistudymentor.question.dto;

import com.aistudymentor.question.entity.AnswerReportReason;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerReportRequest {

    @NotNull
    private AnswerReportReason reason;

    @Size(max = 1000)
    private String comment;
}
