package com.aistudymentor.question.service;

import com.aistudymentor.ai.dto.ExplanationStep;
import com.aistudymentor.ai.dto.GeminiStructuredExplanation;
import com.aistudymentor.ai.repository.AiGenerationLogRepository;
import com.aistudymentor.ai.service.GeminiAdapterService;
import com.aistudymentor.question.dto.QuestionCreateRequest;
import com.aistudymentor.question.entity.Answer;
import com.aistudymentor.profile.entity.StudentProfile;
import com.aistudymentor.profile.repository.StudentProfileRepository;
import com.aistudymentor.question.entity.Question;
import com.aistudymentor.question.entity.QuestionBookmark;
import com.aistudymentor.question.repository.AnswerRepository;
import com.aistudymentor.question.repository.QuestionBookmarkRepository;
import com.aistudymentor.question.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceBookmarkTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionBookmarkRepository bookmarkRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private AiGenerationLogRepository aiGenerationLogRepository;
    @Mock
    private GeminiAdapterService geminiAdapterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(
                questionRepository,
                answerRepository,
                bookmarkRepository,
                profileRepository,
                aiGenerationLogRepository,
                geminiAdapterService,
                objectMapper);
    }

    @Test
    void deletingAnExistingBookmarkTwiceLeavesItRemoved() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        Question question = Question.builder().questionId(questionId).profile(profile).build();
        QuestionBookmark bookmark = QuestionBookmark.builder().profile(profile).question(question).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(questionRepository.findByQuestionIdAndProfile(questionId, profile)).thenReturn(Optional.of(question));
        when(bookmarkRepository.findByProfileAndQuestion(profile, question))
                .thenReturn(Optional.of(bookmark), Optional.empty());

        assertFalse(questionService.setBookmark(userId, questionId, false).isBookmarked());
        assertFalse(questionService.setBookmark(userId, questionId, false).isBookmarked());

        verify(bookmarkRepository).delete(bookmark);
        verify(bookmarkRepository, never()).save(any(QuestionBookmark.class));
    }

    @Test
    void addingAnExistingBookmarkDoesNotCreateAnotherRow() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        Question question = Question.builder().questionId(questionId).profile(profile).build();
        QuestionBookmark bookmark = QuestionBookmark.builder().profile(profile).question(question).build();
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(questionRepository.findByQuestionIdAndProfile(questionId, profile)).thenReturn(Optional.of(question));
        when(bookmarkRepository.findByProfileAndQuestion(profile, question))
                .thenReturn(Optional.of(bookmark));

        assertTrue(questionService.setBookmark(userId, questionId, true).isBookmarked());
        assertTrue(questionService.setBookmark(userId, questionId, true).isBookmarked());

        verify(bookmarkRepository, never()).save(any(QuestionBookmark.class));
        verify(bookmarkRepository, never()).delete(any(QuestionBookmark.class));
    }

    @Test
    void historyQuestionUsesGeminiAndReturnsItsContextualExplanation() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        StudentProfile profile = StudentProfile.builder().profileId(UUID.randomUUID()).build();
        String questionText = "Explain the economic and political causes of the French Revolution.";
        when(profileRepository.findByUser_UserId(userId)).thenReturn(Optional.of(profile));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setQuestionId(questionId);
            return question;
        });
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiAdapterService.generateExplanation("History", questionText, false))
                .thenReturn(GeminiStructuredExplanation.builder()
                        .summary("The Revolution grew from a fiscal crisis and unequal political privileges.")
                        .steps(List.of(
                                ExplanationStep.builder()
                                        .stepNumber(1)
                                        .title("Understand the fiscal crisis")
                                        .explanation("War debts and an unfair tax system strained the monarchy.")
                                        .build(),
                                ExplanationStep.builder()
                                        .stepNumber(2)
                                        .title("Connect economic pressure to political change")
                                        .explanation("Attempts to reform taxation brought the Estates-General together in 1789.")
                                        .build()))
                        .finalAnswer("Economic hardship and resistance to unequal privileges helped trigger "
                                + "the French Revolution.")
                        .keyTakeaways(List.of("Financial pressures intensified demands for reform."))
                        .build());

        var response = questionService.submitQuestion(userId, QuestionCreateRequest.builder()
                .subject("History")
                .questionText(questionText)
                .build());

        assertEquals("History", response.getSubject());
        assertFalse(response.getAnswer().isDemoResponse());
        assertEquals("The Revolution grew from a fiscal crisis and unequal political privileges.",
                response.getAnswer().getStructuredExplanation().getSummary());
        assertEquals(2, response.getAnswer().getStructuredExplanation().getSteps().size());
        assertEquals("Economic hardship and resistance to unequal privileges helped trigger the French Revolution.",
                response.getAnswer().getStructuredExplanation().getFinalAnswer());
        verify(geminiAdapterService).generateExplanation("History", questionText, false);
    }
}
