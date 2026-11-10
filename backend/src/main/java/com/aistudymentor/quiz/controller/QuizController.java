package com.aistudymentor.quiz.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.quiz.dto.QuizAttemptStartResponse;
import com.aistudymentor.quiz.dto.QuizAttemptHistoryDto;
import com.aistudymentor.quiz.dto.QuizCreateRequest;
import com.aistudymentor.quiz.dto.QuizResponseDto;
import com.aistudymentor.quiz.dto.QuizSubmitRequest;
import com.aistudymentor.quiz.dto.QuizSubmitResponse;
import com.aistudymentor.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponseDto> generateQuiz(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuizCreateRequest request
    ) {
        QuizResponseDto response = quizService.generateQuiz(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<QuizResponseDto>> listQuizzes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResponseDto> response = quizService.listQuizzes(principal.getUserId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attempts")
    public ResponseEntity<List<QuizAttemptHistoryDto>> listCompletedAttempts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(quizService.listCompletedAttempts(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponseDto> getQuiz(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID quizId
    ) {
        QuizResponseDto response = quizService.getQuiz(principal.getUserId(), quizId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/attempts")
    public ResponseEntity<QuizAttemptStartResponse> startAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID quizId
    ) {
        QuizAttemptStartResponse response = quizService.startAttempt(principal.getUserId(), quizId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/attempts/{attemptId}/submit")
    public ResponseEntity<QuizSubmitResponse> submitAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID quizId,
            @PathVariable("attemptId") UUID attemptId,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        QuizSubmitResponse response = quizService.submitAttempt(principal.getUserId(), quizId, attemptId, request);
        return ResponseEntity.ok(response);
    }
}
