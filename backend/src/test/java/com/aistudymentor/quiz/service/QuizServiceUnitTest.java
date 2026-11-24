package com.aistudymentor.quiz.service;

import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.GeneratedQuizQuestion;
import com.aistudymentor.ai.service.AiExecution;
import com.aistudymentor.ai.service.AiOrchestrator;
import com.aistudymentor.common.exception.BusinessException;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.quiz.dto.QuizCreateRequest;
import com.aistudymentor.quiz.dto.QuizQuestionEditDto;
import com.aistudymentor.quiz.dto.QuizResponseDto;
import com.aistudymentor.quiz.dto.QuizResponseItemDto;
import com.aistudymentor.quiz.dto.QuizSubmitRequest;
import com.aistudymentor.quiz.dto.QuizUpdateRequest;
import com.aistudymentor.quiz.entity.PracticeQuiz;
import com.aistudymentor.quiz.entity.QuizAttempt;
import com.aistudymentor.quiz.entity.QuizOption;
import com.aistudymentor.quiz.entity.QuizQuestion;
import com.aistudymentor.quiz.entity.QuizStatus;
import com.aistudymentor.quiz.repository.PracticeQuizRepository;
import com.aistudymentor.quiz.repository.QuizAttemptRepository;
import com.aistudymentor.quiz.repository.QuizOptionRepository;
import com.aistudymentor.quiz.repository.QuizQuestionRepository;
import com.aistudymentor.quiz.repository.QuizResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private AiOrchestrator aiOrchestrator;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(
                quizRepository, questionRepository, optionRepository, attemptRepository,
                responseRepository, profileRepository, aiOrchestrator);
    }

    @Test
    @DisplayName("Quiz generation succeeds and strips correct answer from DTO")
    void testGenerateQuizStripsCorrectAnswer() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder()
                .profileId(UUID.randomUUID())
                .educationLevel("High School")
                .build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.save(any(PracticeQuiz.class))).thenAnswer(inv -> {
            PracticeQuiz quiz = inv.getArgument(0);
            quiz.setQuizId(quizId);
            return quiz;
        });
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(optionRepository.save(any(QuizOption.class))).thenAnswer(inv -> inv.getArgument(0));
        GeneratedQuiz generated = GeneratedQuiz.builder().questions(List.of(
                GeneratedQuizQuestion.builder()
                        .type("MCQ")
                        .prompt("What is 2 + 2?")
                        .options(List.of("3", "4", "5"))
                        .correctAnswer("4")
                        .explanation("Add two and two to get four.")
                        .build(),
                GeneratedQuizQuestion.builder()
                        .type("MCQ")
                        .prompt("What is 3 + 3?")
                        .options(List.of("5", "6", "7"))
                        .correctAnswer("6")
                        .explanation("Add three and three to get six.")
                        .build()
        )).build();
        when(aiOrchestrator.generateQuiz(eq(profile), any(), eq(quizId)))
                .thenReturn(new AiExecution<>(generated, null, 0));

        QuizCreateRequest request = QuizCreateRequest.builder()
                .subject("Mathematics")
                .topic("Fractions")
                .numQuestions(2)
                .build();

        QuizResponseDto response = quizService.generateQuiz(userId, request);
        assertNotNull(response);
        assertEquals("Fractions", response.getTopic());
        assertEquals("What is 2 + 2?", response.getQuestions().get(0).getPrompt());
        assertEquals(3, response.getQuestions().get(0).getOptions().size());
        verify(aiOrchestrator).generateQuiz(eq(profile), argThat(aiRequest ->
                "Fractions".equals(aiRequest.getTopic())), eq(quizId));
    }

    @Test
    void providerTimeoutCreatesStructuredStaticFallbackQuiz() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder()
                .profileId(UUID.randomUUID())
                .educationLevel("High School")
                .build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.save(any(PracticeQuiz.class))).thenAnswer(invocation -> {
            PracticeQuiz quiz = invocation.getArgument(0);
            quiz.setQuizId(quizId);
            return quiz;
        });
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(optionRepository.save(any(QuizOption.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiOrchestrator.generateQuiz(eq(profile), any(), eq(quizId)))
                .thenThrow(new BusinessException(HttpStatus.GATEWAY_TIMEOUT,
                        "AI_PROVIDER_TIMEOUT", "The provider timed out."));

        QuizResponseDto response = quizService.generateQuiz(userId, QuizCreateRequest.builder()
                .subject("History")
                .topic("Ancient Rome")
                .numQuestions(5)
                .build());

        assertEquals(5, response.getQuestions().size());
        assertEquals(3, response.getQuestions().get(0).getOptions().size());
        assertEquals("Ancient Rome", response.getTopic());
        ArgumentCaptor<QuizQuestion> savedQuestions = ArgumentCaptor.forClass(QuizQuestion.class);
        verify(questionRepository, org.mockito.Mockito.times(5)).save(savedQuestions.capture());
        for (QuizQuestion question : savedQuestions.getAllValues()) {
            assertNotNull(question.getCorrectAnswer());
            assertNotNull(question.getExplanation());
            assertEquals(3, question.getOptions().size());
            assertEquals(1, question.getOptions().stream()
                    .filter(option -> option.getOptionText().equals(question.getCorrectAnswer()))
                    .count());
        }
    }

    @Test
    void blockedQuizGenerationDoesNotFallBackAroundSafetyRejection() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder()
                .profileId(UUID.randomUUID())
                .educationLevel("High School")
                .build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.save(any(PracticeQuiz.class))).thenAnswer(invocation -> {
            PracticeQuiz quiz = invocation.getArgument(0);
            quiz.setQuizId(quizId);
            return quiz;
        });
        when(aiOrchestrator.generateQuiz(eq(profile), any(), eq(quizId)))
                .thenThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "AI_BLOCKED_CONTENT", "This quiz cannot be generated."));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> quizService.generateQuiz(userId, QuizCreateRequest.builder()
                        .subject("History")
                        .numQuestions(2)
                        .build()));

        assertEquals("AI_BLOCKED_CONTENT", failure.getCode());
        verify(questionRepository, never()).save(any(QuizQuestion.class));
    }

    @Test
    void updateAndDeleteAreRejectedAfterAnAttemptHasStarted() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder()
                .quizId(quizId)
                .profile(profile)
                .status(QuizStatus.IN_PROGRESS)
                .build();
        quiz.setAttempts(List.of(QuizAttempt.builder().quiz(quiz).profile(profile).build()));
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.findByQuizIdAndProfile(quizId, profile)).thenReturn(Optional.of(quiz));

        BusinessException exception = assertThrows(BusinessException.class, () -> quizService.updateQuiz(
                userId, quizId, QuizUpdateRequest.builder().subject("Updated").build()));

        assertEquals("QUIZ_ALREADY_STARTED", exception.getCode());
        assertThrows(BusinessException.class, () -> quizService.deleteQuiz(userId, quizId));
        verify(quizRepository, never()).delete(any(PracticeQuiz.class));
    }

    @Test
    void quizCanBeRenamedAndDeletedBeforeAnyAttemptStarts() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder()
                .quizId(quizId)
                .profile(profile)
                .subject("Mathematics")
                .status(QuizStatus.GENERATED)
                .build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.findByQuizIdAndProfile(quizId, profile)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        QuizResponseDto updated = quizService.updateQuiz(
                userId, quizId, QuizUpdateRequest.builder().subject("Physics").build());
        quizService.deleteQuiz(userId, quizId);

        assertEquals("Physics", updated.getSubject());
        verify(quizRepository).delete(quiz);
    }

    @Test
    void quizQuestionsCanBeEditedAddedOrRemovedBeforeStarting() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder()
                .quizId(quizId)
                .profile(profile)
                .subject("History")
                .topic("Ancient Rome")
                .status(QuizStatus.GENERATED)
                .questions(new java.util.ArrayList<>())
                .build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(quizRepository.findByQuizIdAndProfile(quizId, profile)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(optionRepository.save(any(QuizOption.class))).thenAnswer(inv -> inv.getArgument(0));
        QuizUpdateRequest update = QuizUpdateRequest.builder()
                .subject("History")
                .topic("Roman Republic")
                .questions(List.of(QuizQuestionEditDto.builder()
                        .prompt("Which institution elected Roman magistrates?")
                        .options(List.of("The Senate", "The Assemblies", "The Consuls"))
                        .correctAnswer("The Assemblies")
                        .explanation("Citizens gathered in assemblies to elect magistrates.")
                        .build()))
                .build();

        QuizResponseDto response = quizService.updateQuiz(userId, quizId, update);
        var editor = quizService.getQuizForEdit(userId, quizId);

        assertEquals("Roman Republic", response.getTopic());
        assertEquals(1, response.getQuestions().size());
        assertEquals("The Assemblies", editor.getQuestions().get(0).getCorrectAnswer());
        assertEquals("Citizens gathered in assemblies to elect magistrates.",
                editor.getQuestions().get(0).getExplanation());
        assertEquals(3, editor.getQuestions().get(0).getOptions().size());
    }

    @Test
    void submitRejectsAttemptWhenQuizPathDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder().quizId(UUID.randomUUID()).build();
        QuizAttempt attempt = QuizAttempt.builder().attemptId(UUID.randomUUID()).quiz(quiz).profile(profile).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(attemptRepository.findById(attempt.getAttemptId())).thenReturn(Optional.of(attempt));

        BusinessException exception = assertThrows(BusinessException.class, () -> quizService.submitAttempt(
                userId,
                quizId,
                attempt.getAttemptId(),
                QuizSubmitRequest.builder().responses(List.of()).build()));

        assertEquals("ATTEMPT_NOT_FOUND", exception.getCode());
        verify(responseRepository, never()).save(any());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitRejectsAnOptionBelongingToAnotherQuestion() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID otherQuestionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder().quizId(quizId).build();
        QuizQuestion question = QuizQuestion.builder()
                .questionId(questionId)
                .quiz(quiz)
                .correctAnswer("Option B")
                .build();
        quiz.setQuestions(List.of(question));
        QuizQuestion otherQuestion = QuizQuestion.builder().questionId(otherQuestionId).quiz(quiz).build();
        QuizOption foreignOption = QuizOption.builder()
                .optionId(optionId)
                .question(otherQuestion)
                .optionText("Option B")
                .build();
        QuizAttempt attempt = QuizAttempt.builder().attemptId(attemptId).quiz(quiz).profile(profile).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(optionRepository.findById(optionId)).thenReturn(Optional.of(foreignOption));

        BusinessException exception = assertThrows(BusinessException.class, () -> quizService.submitAttempt(
                userId,
                quizId,
                attemptId,
                QuizSubmitRequest.builder().responses(List.of(QuizResponseItemDto.builder()
                        .questionId(questionId)
                        .selectedOptionId(optionId)
                        .build())).build()));

        assertEquals("INVALID_QUIZ_RESPONSE", exception.getCode());
        verify(responseRepository, never()).save(any());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void gradingIncludesExplanationsAndMarksOmittedQuestionsIncorrect() {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        PracticeQuiz quiz = PracticeQuiz.builder().quizId(quizId).build();
        QuizQuestion question = QuizQuestion.builder()
                .questionId(questionId)
                .quiz(quiz)
                .type(com.aistudymentor.quiz.entity.QuizQuestionType.MCQ)
                .correctAnswer("PARIS")
                .explanation("Paris became France's capital during the medieval period.")
                .build();
        quiz.setQuestions(List.of(question));
        QuizOption option = QuizOption.builder()
                .optionId(optionId)
                .question(question)
                .optionText("Paris")
                .build();
        QuizAttempt attempt = QuizAttempt.builder().attemptId(attemptId).quiz(quiz).profile(profile).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(optionRepository.findById(optionId)).thenReturn(Optional.of(option));

        var correctResult = quizService.submitAttempt(userId, quizId, attemptId, QuizSubmitRequest.builder()
                .responses(List.of(QuizResponseItemDto.builder()
                        .questionId(questionId)
                        .selectedOptionId(optionId)
                        .build()))
                .build());

        assertEquals(100, correctResult.getScore());
        assertEquals("Paris became France's capital during the medieval period.",
                correctResult.getResults().get(0).getExplanation());

        QuizAttempt unansweredAttempt = QuizAttempt.builder()
                .attemptId(UUID.randomUUID())
                .quiz(quiz)
                .profile(profile)
                .build();
        when(attemptRepository.findById(unansweredAttempt.getAttemptId())).thenReturn(Optional.of(unansweredAttempt));
        var unansweredResult = quizService.submitAttempt(userId, quizId, unansweredAttempt.getAttemptId(),
                QuizSubmitRequest.builder().responses(List.of()).build());

        assertEquals(0, unansweredResult.getScore());
        assertEquals(false, unansweredResult.getResults().get(0).getIsCorrect());
        assertEquals("Paris became France's capital during the medieval period.",
                unansweredResult.getResults().get(0).getExplanation());
    }
}
