package com.aistudymentor.dashboard.service;

import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.dashboard.dto.DashboardMetricsResponse;
import com.aistudymentor.dashboard.dto.SubjectBreakdownDto;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.repository.QuestionRepository;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import com.aistudymentor.quiz.entity.QuizAttempt;
import com.aistudymentor.quiz.entity.QuizStatus;
import com.aistudymentor.quiz.repository.PracticeQuizRepository;
import com.aistudymentor.quiz.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final QuestionRepository questionRepository;
    private final PracticeQuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final StudentProfileRepository profileRepository;

    @Cacheable(value = "dashboard-metrics", key = "#userId")
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics(UUID userId) {
        StudentProfile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Student profile not found."));

        long totalQuestions = questionRepository.countByProfile(profile);
        long completedQuizzes = quizRepository.countByProfileAndStatus(profile, QuizStatus.COMPLETED);

        List<QuizAttempt> attempts = attemptRepository.findByProfileOrderByCreatedAtDesc(profile);
        double overallAccuracy = attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0.0);

        // Subject Breakdown
        List<Question> userQuestions = questionRepository.findByProfile(profile);
        Map<String, Long> questionCounts = userQuestions.stream()
                .collect(Collectors.groupingBy(Question::getSubject, Collectors.counting()));

        List<PracticeQuiz> userQuizzes = quizRepository.findByProfile(profile);
        Map<String, Long> quizCounts = userQuizzes.stream()
                .collect(Collectors.groupingBy(PracticeQuiz::getSubject, Collectors.counting()));

        Map<String, List<QuizAttempt>> attemptsBySubject = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getQuiz().getSubject()));

        List<SubjectBreakdownDto> breakdown = new ArrayList<>();
        java.util.Set<String> allSubjects = new java.util.HashSet<>();
        allSubjects.addAll(questionCounts.keySet());
        allSubjects.addAll(quizCounts.keySet());

        for (String subject : allSubjects) {
            long qCount = questionCounts.getOrDefault(subject, 0L);
            long quizCount = quizCounts.getOrDefault(subject, 0L);
            List<QuizAttempt> subAttempts = attemptsBySubject.getOrDefault(subject, List.of());
            double subAccuracy = subAttempts.stream()
                    .mapToInt(QuizAttempt::getScore)
                    .average()
                    .orElse(0.0);

            breakdown.add(SubjectBreakdownDto.builder()
                    .subject(subject)
                    .questionsCount(qCount)
                    .quizCount(quizCount)
                    .accuracyRate(Math.round(subAccuracy * 10.0) / 10.0)
                    .build());
        }

        return DashboardMetricsResponse.builder()
                .totalQuestionsAsked(totalQuestions)
                .totalQuizzesCompleted(completedQuizzes)
                .overallAccuracyRate(Math.round(overallAccuracy * 10.0) / 10.0)
                .subjectBreakdown(breakdown)
                .build();
    }
}
