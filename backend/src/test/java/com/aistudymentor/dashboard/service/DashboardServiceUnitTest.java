package com.aistudymentor.dashboard.service;

import com.aistudymentor.dashboard.dto.DashboardMetricsResponse;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.question.repository.QuestionRepository;
import com.aistudymentor.quiz.repository.PracticeQuizRepository;
import com.aistudymentor.quiz.repository.QuizAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceUnitTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private PracticeQuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository attemptRepository;
    @Mock
    private StudentProfileRepository profileRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(questionRepository, quizRepository, attemptRepository, profileRepository);
    }

    @Test
    @DisplayName("Dashboard metrics calculation aggregates counts and accuracy correctly")
    void testGetMetricsCalculatesCorrectly() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(questionRepository.countByProfile(profile)).thenReturn(10L);
        when(quizRepository.countByProfileAndStatus(any(), any())).thenReturn(3L);
        when(attemptRepository.findByProfileOrderByCreatedAtDesc(profile)).thenReturn(List.of());
        when(questionRepository.findByProfile(profile)).thenReturn(List.of());
        when(quizRepository.findByProfile(profile)).thenReturn(List.of());

        DashboardMetricsResponse metrics = dashboardService.getMetrics(userId);
        assertEquals(10L, metrics.getTotalQuestionsAsked());
        assertEquals(3L, metrics.getTotalQuizzesCompleted());
    }
}
