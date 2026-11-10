package com.aistudymentor.question.controller;

import com.aistudymentor.config.UserPrincipal;
import com.aistudymentor.question.dto.AnswerResponse;
import com.aistudymentor.question.dto.QuestionBookmarkResponse;
import com.aistudymentor.question.dto.QuestionCreateRequest;
import com.aistudymentor.question.dto.QuestionResponse;
import com.aistudymentor.question.dto.QuestionSummaryResponse;
import com.aistudymentor.question.dto.UploadAuthorizeRequest;
import com.aistudymentor.question.dto.UploadAuthorizeResponse;
import com.aistudymentor.question.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/uploads/authorize")
    public ResponseEntity<UploadAuthorizeResponse> authorizeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UploadAuthorizeRequest request
    ) {
        UploadAuthorizeResponse response = questionService.authorizeUpload(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/questions")
    public ResponseEntity<QuestionResponse> submitQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        QuestionResponse response = questionService.submitQuestion(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/questions")
    public ResponseEntity<Page<QuestionSummaryResponse>> listQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionSummaryResponse> response = questionService.listQuestions(principal.getUserId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID questionId
    ) {
        QuestionResponse response = questionService.getQuestion(principal.getUserId(), questionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/questions/{id}/alternative-explanation")
    public ResponseEntity<AnswerResponse> requestAlternativeExplanation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID questionId
    ) {
        AnswerResponse response = questionService.requestAlternativeExplanation(principal.getUserId(), questionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/questions/{id}/bookmark")
    public ResponseEntity<QuestionBookmarkResponse> addBookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID questionId
    ) {
        QuestionBookmarkResponse response = questionService.toggleBookmark(principal.getUserId(), questionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/questions/{id}/bookmark")
    public ResponseEntity<QuestionBookmarkResponse> removeBookmark(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID questionId
    ) {
        QuestionBookmarkResponse response = questionService.toggleBookmark(principal.getUserId(), questionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/bookmarks")
    public ResponseEntity<Page<QuestionSummaryResponse>> listBookmarks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionSummaryResponse> response = questionService.listBookmarks(principal.getUserId(), pageable);
        return ResponseEntity.ok(response);
    }
}
