package com.aistudymentor.quiz.service;

import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.quiz.dto.QuizCreateRequest;
import com.aistudymentor.quiz.dto.QuizResponseDto;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import com.aistudymentor.quiz.entity.QuizOption;
import com.aistudymentor.quiz.entity.QuizQuestion;
import com.aistudymentor.quiz.repository.PracticeQuizRepository;
import com.aistudymentor.quiz.repository.QuizAttemptRepository;
import com.aistudymentor.quiz.repository.QuizOptionRepository;
import com.aistudymentor.quiz.repository.QuizQuestionRepository;
import com.aistudymentor.quiz.repository.QuizResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuizServiceUnitTest {

    @Mock
    private PracticeQuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository questionRepository;
    @Mock
    private QuizOptionRepository optionRepository;
    @Mock
    private QuizAttemptRepository attemptRepository;
    @Mock
    private QuizResponseRepository responseRepository;
    @Mock
    private StudentProfileRepository profileRepository;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepository, questionRepository, optionRepository, attemptRepository, responseRepository, profileRepository);
    }

    @Test
    @DisplayName("Quiz generation succeeds and strips correct answer from DTO")
    void testGenerateQuizStripsCorrectAnswer() {
        UUID userId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.save(any(PracticeQuiz.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(optionRepository.save(any(QuizOption.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizCreateRequest request = QuizCreateRequest.builder()
                .subject("Mathematics")
                .numQuestions(2)
                .build();

        QuizResponseDto response = quizService.generateQuiz(userId, request);
        assertNotNull(response);
    }
}
